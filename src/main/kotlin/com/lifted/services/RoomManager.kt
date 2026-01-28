package com.lifted.services

import com.lifted.models.Platform
import com.lifted.models.Room
import com.lifted.models.User
import com.lifted.models.VideoState
import io.ktor.websocket.*
import java.util.concurrent.ConcurrentHashMap

object RoomManager {
    private val rooms = ConcurrentHashMap<String, Room>()
    private val userToRoom = ConcurrentHashMap<String, String>()

    private const val ROOM_CODE_LENGTH = 4
    private val ROOM_CODE_CHARS = ('A'..'Z')

    fun createRoom(hostId: String, hostName: String, platform: Platform, session: WebSocketSession): Room {
        val roomId = generateRoomId()
        val host = User(hostId, hostName, session)
        val room = Room(
            id = roomId,
            platform = platform,
            hostId = hostId
        )
        room.users[hostId] = host
        rooms[roomId] = room
        userToRoom[hostId] = roomId
        return room
    }

    fun joinRoom(roomId: String, userId: String, userName: String, session: WebSocketSession): Room? {
        val room = rooms[roomId] ?: return null
        val user = User(userId, userName, session)
        room.users[userId] = user
        userToRoom[userId] = roomId
        return room
    }

    fun leaveRoom(userId: String): Pair<Room?, User?> {
        val roomId = userToRoom.remove(userId) ?: return null to null
        val room = rooms[roomId] ?: return null to null
        val user = room.users.remove(userId) ?: return room to null

        // Clean up empty rooms
        if (room.users.isEmpty()) {
            rooms.remove(roomId)
        }

        return room to user
    }

    fun getRoomForUser(userId: String): Room? {
        val roomId = userToRoom[userId] ?: return null
        return rooms[roomId]
    }

    fun updateRoomState(roomId: String, state: VideoState, currentTime: Double) {
        rooms[roomId]?.let { room ->
            room.currentState = state
            room.currentTime = currentTime
        }
    }

    fun getUsersInRoom(roomId: String): List<User> {
        return rooms[roomId]?.users?.values?.toList() ?: emptyList()
    }

    private fun generateRoomId(): String {
        while (true) {
            val code = (1..ROOM_CODE_LENGTH).map { ROOM_CODE_CHARS.random() }.joinToString("")
            if (!rooms.containsKey(code)) return code
        }
    }
}