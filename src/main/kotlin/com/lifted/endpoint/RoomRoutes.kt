package com.lifted.endpoint

import com.lifted.data.model.Room
import com.lifted.data.websocket.CreateRoomMessage
import com.lifted.data.websocket.ErrorMessage
import com.lifted.data.websocket.HeartbeatMessage
import com.lifted.data.websocket.JoinRoomMessage
import com.lifted.data.websocket.LeaveRoomMessage
import com.lifted.data.websocket.MessageParser
import com.lifted.data.websocket.NavigateMessage
import com.lifted.data.websocket.NavigateUpdateMessage
import com.lifted.data.websocket.RoomCreatedMessage
import com.lifted.data.websocket.RoomJoinedMessage
import com.lifted.data.websocket.SyncUpdateMessage
import com.lifted.data.websocket.UserJoinedMessage
import com.lifted.data.websocket.UserLeftMessage
import com.lifted.data.websocket.VideoUpdateMessage
import com.lifted.data.model.toDto
import com.lifted.plugin.wsJson
import com.lifted.service.NavigateResult
import com.lifted.service.RoomService
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory
import java.util.*

private val logger = LoggerFactory.getLogger("com.lifted.endpoint.RoomRoutes")

fun Route.roomRoutes() {
    val roomService by inject<RoomService>()

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
                        is CreateRoomMessage -> handleCreateRoom(roomService, userId, message, this)
                        is JoinRoomMessage -> handleJoinRoom(roomService, userId, message, this)
                        is VideoUpdateMessage -> handleVideoUpdate(roomService, userId, message)
                        is NavigateMessage -> handleNavigate(roomService, userId, message, this)
                        is LeaveRoomMessage -> handleLeaveRoom(roomService, userId)
                        is HeartbeatMessage -> { /* Keep-alive, no action needed */ }
                    }
                }
            }
        } catch (_: ClosedReceiveChannelException) {
            logger.info("Client disconnected: userId=$userId")
        } catch (e: Exception) {
            logger.error("Error handling WebSocket for userId=$userId", e)
        } finally {
            handleDisconnect(roomService, userId)
        }
    }
}

private suspend fun handleCreateRoom(
    roomService: RoomService,
    userId: String,
    message: CreateRoomMessage,
    session: WebSocketSession
) {
    logger.info("Creating room: userId=$userId, userName=${message.userName}, platform=${message.platform}")
    val room = roomService.createRoom(userId, message.userName, message.platform, session, message.currentTime)
    logger.info("Room created: roomId=${room.id}, host=${message.userName}")

    val response = RoomCreatedMessage(roomId = room.id)
    session.send(Frame.Text(wsJson.encodeToString(response)))

    // Also send room_joined to give the host the full room state
    session.send(Frame.Text(wsJson.encodeToString(room.toJoinedMessage())))
}

private suspend fun handleJoinRoom(
    roomService: RoomService,
    userId: String,
    message: JoinRoomMessage,
    session: WebSocketSession
) {
    logger.info("Join room request: userId=$userId, userName=${message.userName}, roomId=${message.roomId}")
    val room = roomService.joinRoom(message.roomId, userId, message.userName, session)

    if (room == null) {
        logger.warn("Room not found: roomId=${message.roomId}")
        sendError(session, "ROOM_NOT_FOUND", "Room ${message.roomId} does not exist")
        return
    }

    logger.info("User joined room: roomId=${room.id}, userName=${message.userName}, userCount=${room.users.size}")

    // Send room state to the joining user
    session.send(Frame.Text(wsJson.encodeToString(room.toJoinedMessage())))

    // Notify other users in the room
    val userJoinedMessage = UserJoinedMessage(
        userName = message.userName,
        userCount = room.users.size,
        users = room.users.values.map { it.toDto() }
    )
    broadcastToRoom(room, wsJson.encodeToString(userJoinedMessage), excludeUserId = userId)
}

private suspend fun handleVideoUpdate(roomService: RoomService, userId: String, message: VideoUpdateMessage) {
    val room = roomService.updateState(userId, message.state, message.currentTime) ?: return

    logger.debug(
        "Video update: roomId={}, userId={}, state={}, time={}",
        room.id,
        userId,
        message.state,
        message.currentTime
    )

    // Broadcast to other users
    val syncMessage = SyncUpdateMessage(
        state = message.state,
        currentTime = message.currentTime,
        fromUserId = userId
    )
    broadcastToRoom(room, wsJson.encodeToString(syncMessage), excludeUserId = userId)
}

private suspend fun handleNavigate(
    roomService: RoomService,
    userId: String,
    message: NavigateMessage,
    session: WebSocketSession
) {
    logger.info("Navigate request: userId=$userId, url=${message.url}")

    when (val result = roomService.navigate(userId, message.url)) {
        is NavigateResult.NotInRoom -> return
        is NavigateResult.InvalidUrl -> {
            logger.warn("Invalid URL for platform: platform=${result.platform}, url=${message.url}")
            sendError(session, "INVALID_URL", "URL does not match room platform (${result.platform})")
        }
        is NavigateResult.Success -> {
            val room = result.room
            val navigateMessage = NavigateUpdateMessage(
                url = message.url,
                fromUserId = userId
            )
            broadcastToRoom(room, wsJson.encodeToString(navigateMessage))

            // Tell everyone the room was reset for the new video
            val syncReset = SyncUpdateMessage(
                state = room.currentState,
                currentTime = room.currentTime,
                fromUserId = userId
            )
            broadcastToRoom(room, wsJson.encodeToString(syncReset))
        }
    }
}

private suspend fun handleLeaveRoom(roomService: RoomService, userId: String) {
    logger.info("User leaving room: userId=$userId")
    handleDisconnect(roomService, userId)
}

private suspend fun handleDisconnect(roomService: RoomService, userId: String) {
    val (room, user) = roomService.leaveRoom(userId) ?: return

    logger.info("User disconnected from room: roomId=${room.id}, userName=${user.name}, remainingUsers=${room.users.size}")
    val leftMessage = UserLeftMessage(
        userName = user.name,
        userCount = room.users.size,
        users = room.users.values.map { it.toDto() }
    )
    broadcastToRoom(room, wsJson.encodeToString(leftMessage))
}

private fun Room.toJoinedMessage() = RoomJoinedMessage(
    roomId = id,
    platform = platform,
    state = currentState,
    currentTime = currentTime,
    users = users.values.map { it.toDto() }
)

private suspend fun broadcastToRoom(
    room: Room,
    jsonMessage: String,
    excludeUserId: String? = null
) {
    logger.debug("Broadcasting to room ${room.id} (excluding $excludeUserId): $jsonMessage")

    room.users.values.forEach { user ->
        if (user.id != excludeUserId) {
            try {
                user.session.send(Frame.Text(jsonMessage))
            } catch (e: Exception) {
                // Only stop if our own coroutine was canceled; a peer's failed send must not drop the sender
                currentCoroutineContext().ensureActive()
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
