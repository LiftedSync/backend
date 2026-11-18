package com.lifted.data.websocket

import com.lifted.data.dto.UserDto
import com.lifted.data.model.Platform
import com.lifted.data.model.VideoState
import com.lifted.plugin.wsJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory

// ============== Client -> Server Messages ==============

@Serializable
data class JoinRoomMessage(
    val type: String = "join_room",
    val roomId: String,
    val userName: String
)

@Serializable
data class CreateRoomMessage(
    val type: String = "create_room",
    val userName: String,
    val platform: Platform,
    val currentTime: Double = 0.0
)

@Serializable
data class VideoUpdateMessage(
    val type: String = "video_update",
    val state: VideoState,
    val currentTime: Double
)

@Serializable
data class HeartbeatMessage(
    val type: String = "heartbeat"
)

@Serializable
data class LeaveRoomMessage(
    val type: String = "leave_room"
)

@Serializable
data class NavigateMessage(
    val type: String = "navigate",
    val url: String
)

// ============== Server -> Client Messages ==============

@Serializable
data class RoomJoinedMessage(
    val type: String = "room_joined",
    val roomId: String,
    val platform: Platform,
    val state: VideoState,
    val currentTime: Double,
    val users: List<UserDto>
)

@Serializable
data class RoomCreatedMessage(
    val type: String = "room_created",
    val roomId: String
)

@Serializable
data class SyncUpdateMessage(
    val type: String = "sync_update",
    val state: VideoState,
    val currentTime: Double,
    val fromUserId: String
)

@Serializable
data class UserJoinedMessage(
    val type: String = "user_joined",
    val userName: String,
    val userCount: Int,
    val users: List<UserDto>
)

@Serializable
data class UserLeftMessage(
    val type: String = "user_left",
    val userName: String,
    val userCount: Int,
    val users: List<UserDto>
)

@Serializable
data class ErrorMessage(
    val type: String = "error",
    val code: String,
    val message: String
)

@Serializable
data class NavigateUpdateMessage(
    val type: String = "navigate_update",
    val url: String,
    val fromUserId: String
)

// Helper object for parsing incoming messages
object MessageParser {
    private val logger = LoggerFactory.getLogger(MessageParser::class.java)

    fun parseClientMessage(json: String): Any? {
        return try {
            val jsonElement = wsJson.parseToJsonElement(json)
            val type = jsonElement.jsonObject["type"]?.jsonPrimitive?.content

            when (type) {
                "join_room" -> wsJson.decodeFromString<JoinRoomMessage>(json)
                "create_room" -> wsJson.decodeFromString<CreateRoomMessage>(json)
                "video_update" -> wsJson.decodeFromString<VideoUpdateMessage>(json)
                "heartbeat" -> wsJson.decodeFromString<HeartbeatMessage>(json)
                "leave_room" -> wsJson.decodeFromString<LeaveRoomMessage>(json)
                "navigate" -> wsJson.decodeFromString<NavigateMessage>(json)
                else -> null
            }
        } catch (e: Exception) {
            logger.warn("Failed to parse client message: {}", json, e)
            null
        }
    }
}