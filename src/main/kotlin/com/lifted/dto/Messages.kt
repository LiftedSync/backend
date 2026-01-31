package com.lifted.dto

import com.lifted.models.Platform
import com.lifted.models.VideoState
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
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
    val users: List<UserInfo>
)

@Serializable
data class UserInfo(
    val id: String,
    val name: String
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
    val users: List<UserInfo>
)

@Serializable
data class UserLeftMessage(
    val type: String = "user_left",
    val userName: String,
    val userCount: Int,
    val users: List<UserInfo>
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
            val jsonElement = Json.parseToJsonElement(json)
            val type = jsonElement.jsonObject["type"]?.jsonPrimitive?.content

            when (type) {
                "join_room" -> Json.decodeFromString<JoinRoomMessage>(json)
                "create_room" -> Json.decodeFromString<CreateRoomMessage>(json)
                "video_update" -> Json.decodeFromString<VideoUpdateMessage>(json)
                "heartbeat" -> Json.decodeFromString<HeartbeatMessage>(json)
                "leave_room" -> Json.decodeFromString<LeaveRoomMessage>(json)
                "navigate" -> Json.decodeFromString<NavigateMessage>(json)
                else -> null
            }
        } catch (e: Exception) {
            logger.warn("Failed to parse client message: {}", json, e)
            null
        }
    }
}