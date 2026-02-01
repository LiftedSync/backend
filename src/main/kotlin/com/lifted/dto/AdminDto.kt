package com.lifted.dto

import com.lifted.models.Platform
import com.lifted.models.VideoState
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    val name: String
)

@Serializable
data class RoomDto(
    val id: String,
    val platform: Platform,
    val hostId: String,
    val currentState: VideoState,
    val currentTime: Double,
    val users: List<UserDto>
)

@Serializable
data class RoomCountResponse(
    val count: Int
)