package com.lifted.service

import com.lifted.data.model.Platform
import com.lifted.data.model.Room
import com.lifted.data.model.User
import com.lifted.data.model.VideoState
import com.lifted.data.model.matchesUrl
import io.ktor.websocket.*
import java.util.concurrent.ConcurrentHashMap

class RoomService {
    private val rooms = ConcurrentHashMap<String, Room>()
    private val userToRoom = ConcurrentHashMap<String, String>()

    private companion object {
        const val ROOM_CODE_LENGTH = 4
        val ROOM_CODE_CHARS = 'A'..'Z'
    }

    fun createRoom(
        hostId: String,
        hostName: String,
        platform: Platform,
        session: WebSocketSession,
        currentTime: Double = 0.0
    ): Room {
        while (true) {
            val room = Room(
                id = generateRoomCode(),
                platform = platform,
                hostId = hostId,
                currentTime = currentTime
            )
            room.users[hostId] = User(hostId, hostName, session)
            if (rooms.putIfAbsent(room.id, room) == null) {
                userToRoom[hostId] = room.id
                return room
            }
        }
    }

    /** Joins the room with [roomId] (case-insensitive). Returns null if the room does not exist. */
    fun joinRoom(roomId: String, userId: String, userName: String, session: WebSocketSession): Room? {
        val id = roomId.uppercase()
        val room = rooms.computeIfPresent(id) { _, room ->
            room.users[userId] = User(userId, userName, session)
            room
        } ?: return null
        userToRoom[userId] = id
        return room
    }

    /**
     * Removes the user from their room and deletes the room once nobody is left.
     * Returns the room and the user who left, or null if the user was not in a room.
     */
    fun leaveRoom(userId: String): Pair<Room, User>? {
        val roomId = userToRoom.remove(userId) ?: return null
        var result: Pair<Room, User>? = null
        rooms.computeIfPresent(roomId) { _, room ->
            room.users.remove(userId)?.let { result = room to it }
            if (room.users.isEmpty()) null else room
        }
        return result
    }

    fun getRoomForUser(userId: String): Room? = userToRoom[userId]?.let { rooms[it] }

    /** Sets the playback state of the user's room. Returns the room, or null if the user is not in one. */
    fun updateState(userId: String, state: VideoState, currentTime: Double): Room? {
        val room = getRoomForUser(userId) ?: return null
        room.currentState = state
        room.currentTime = currentTime
        return room
    }

    /** Switches the user's room to [url] and resets playback to paused at 0:00. */
    fun navigate(userId: String, url: String): NavigateResult {
        val room = getRoomForUser(userId) ?: return NavigateResult.NotInRoom
        if (!room.platform.matchesUrl(url)) return NavigateResult.InvalidUrl(room.platform)

        room.currentState = VideoState.PAUSED
        room.currentTime = 0.0
        return NavigateResult.Success(room)
    }

    fun getAllRooms(): List<Room> = rooms.values.toList()

    fun getRoomCount(): Int = rooms.size

    private fun generateRoomCode(): String =
        (1..ROOM_CODE_LENGTH).map { ROOM_CODE_CHARS.random() }.joinToString("")
}
