package com.lifted.routes

import com.lifted.dto.*
import com.lifted.models.Platform
import com.lifted.plugins.wsJson
import com.lifted.services.RoomManager
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import org.slf4j.LoggerFactory
import java.util.*

private val logger = LoggerFactory.getLogger("RoomRoutes")

fun Route.roomRoutes() {
    webSocket("/ws") {
        val userId = UUID.randomUUID().toString()
        logger.info("New WebSocket connection: userId=$userId")

        try {
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    logger.debug("Received message from $userId: $text")

                    val message = MessageParser.parseClientMessage(text)
                    if (message == null) {
                        logger.warn("Failed to parse message from $userId: $text")
                        sendError(this, "INVALID_MESSAGE", "Could not parse message")
                        continue
                    }

                    when (message) {
                        is CreateRoomMessage -> handleCreateRoom(userId, message, this)
                        is JoinRoomMessage -> handleJoinRoom(userId, message, this)
                        is VideoUpdateMessage -> handleVideoUpdate(userId, message)
                        is NavigateMessage -> handleNavigate(userId, message, this)
                        is LeaveRoomMessage -> handleLeaveRoom(userId)
                        is HeartbeatMessage -> { /* Keep-alive, no action needed */ }
                    }
                }
            }
        } catch (_: ClosedReceiveChannelException) {
            logger.info("Client disconnected: userId=$userId")
        } catch (e: Exception) {
            logger.error("Error handling WebSocket for userId=$userId", e)
        } finally {
            handleDisconnect(userId)
        }
    }
}

private suspend fun handleCreateRoom(
    userId: String,
    message: CreateRoomMessage,
    session: WebSocketSession
) {
    logger.info("Creating room: userId=$userId, userName=${message.userName}, platform=${message.platform}")
    val room = RoomManager.createRoom(userId, message.userName, message.platform, session)
    logger.info("Room created: roomId=${room.id}, host=${message.userName}")

    val response = RoomCreatedMessage(roomId = room.id)
    session.send(Frame.Text(wsJson.encodeToString(response)))

    // Also send room_joined to give the host the full room state
    val joinedResponse = RoomJoinedMessage(
        roomId = room.id,
        platform = room.platform,
        state = room.currentState,
        currentTime = room.currentTime,
        users = room.users.values.map { UserInfo(it.id, it.name) }
    )
    session.send(Frame.Text(wsJson.encodeToString(joinedResponse)))
}

private suspend fun handleJoinRoom(
    userId: String,
    message: JoinRoomMessage,
    session: WebSocketSession
) {
    logger.info("Join room request: userId=$userId, userName=${message.userName}, roomId=${message.roomId}")
    val room = RoomManager.joinRoom(message.roomId.uppercase(), userId, message.userName, session)

    if (room == null) {
        logger.warn("Room not found: roomId=${message.roomId}")
        sendError(session, "ROOM_NOT_FOUND", "Room ${message.roomId} does not exist")
        return
    }

    logger.info("User joined room: roomId=${room.id}, userName=${message.userName}, userCount=${room.users.size}")

    // Send room state to the joining user
    val joinedResponse = RoomJoinedMessage(
        roomId = room.id,
        platform = room.platform,
        state = room.currentState,
        currentTime = room.currentTime,
        users = room.users.values.map { UserInfo(it.id, it.name) }
    )
    session.send(Frame.Text(wsJson.encodeToString(joinedResponse)))

    // Notify other users in the room
    val userJoinedMessage = UserJoinedMessage(
        userName = message.userName,
        userCount = room.users.size,
        users = room.users.values.map { UserInfo(it.id, it.name) }
    )
    broadcastToRoom(room.id, wsJson.encodeToString(userJoinedMessage), excludeUserId = userId)
}

private suspend fun handleVideoUpdate(userId: String, message: VideoUpdateMessage) {
    val room = RoomManager.getRoomForUser(userId) ?: return

    logger.debug(
        "Video update: roomId={}, userId={}, state={}, time={}",
        room.id,
        userId,
        message.state,
        message.currentTime
    )

    // Update room state
    RoomManager.updateRoomState(room.id, message.state, message.currentTime)

    // Broadcast to other users
    val syncMessage = SyncUpdateMessage(
        state = message.state,
        currentTime = message.currentTime,
        fromUserId = userId
    )
    broadcastToRoom(room.id, wsJson.encodeToString(syncMessage), excludeUserId = userId)
}

private suspend fun handleNavigate(userId: String, message: NavigateMessage, session: WebSocketSession) {
    val room = RoomManager.getRoomForUser(userId) ?: return

    logger.info("Navigate request: roomId=${room.id}, userId=$userId, url=${message.url}")

    if (!isValidUrlForPlatform(message.url, room.platform)) {
        logger.warn("Invalid URL for platform: platform=${room.platform}, url=${message.url}")
        sendError(session, "INVALID_URL", "URL does not match room platform (${room.platform})")
        return
    }

    // Broadcast to ALL users in the room (including the sender)
    val navigateMessage = NavigateUpdateMessage(
        url = message.url,
        fromUserId = userId
    )
    broadcastToRoom(room.id, wsJson.encodeToString(navigateMessage))
}

private suspend fun handleLeaveRoom(userId: String) {
    logger.info("User leaving room: userId=$userId")
    handleDisconnect(userId)
}

private suspend fun handleDisconnect(userId: String) {
    val (room, user) = RoomManager.leaveRoom(userId)

    if (room != null && user != null) {
        logger.info("User disconnected from room: roomId=${room.id}, userName=${user.name}, remainingUsers=${room.users.size}")
        val leftMessage = UserLeftMessage(
            userName = user.name,
            userCount = room.users.size,
            users = room.users.values.map { UserInfo(it.id, it.name) }
        )
        broadcastToRoom(room.id, wsJson.encodeToString(leftMessage))
    }
}

private suspend fun broadcastToRoom(
    roomId: String,
    jsonMessage: String,
    excludeUserId: String? = null
) {
    val users = RoomManager.getUsersInRoom(roomId)

    logger.debug("Broadcasting to room $roomId (excluding $excludeUserId): $jsonMessage")

    users.forEach { user ->
        if (user.id != excludeUserId) {
            try {
                user.session.send(Frame.Text(jsonMessage))
            } catch (e: Exception) {
                logger.warn("Failed to send message to user ${user.id}: ${e.message}")
            }
        }
    }
}

private suspend fun sendError(session: WebSocketSession, code: String, message: String) {
    logger.warn("Sending error: code=$code, message=$message")
    val errorMessage = ErrorMessage(code = code, message = message)
    session.send(Frame.Text(wsJson.encodeToString(errorMessage)))
}

private fun isValidUrlForPlatform(url: String, platform: Platform): Boolean {
    return when (platform) {
        Platform.YOUTUBE -> url.contains("youtube.com") || url.contains("youtu.be")
        Platform.CRUNCHYROLL -> url.contains("crunchyroll.com")
        Platform.NETFLIX -> url.contains("netflix.com")
        Platform.PRIMEVIDEO -> url.contains("primevideo.com") ||
            url.contains("amazon.com/gp/video") || url.contains("amazon.de/gp/video")
    }
}