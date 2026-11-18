package com.lifted.data.dto

import com.lifted.data.model.Platform
import com.lifted.data.model.VideoState
import kotlinx.serialization.Serializable

@Serializable
data class RoomDto(
    val id: String,
    val platform: Platform,
    val hostId: String,
    val currentState: VideoState,
    val currentTime: Double,
    val users: List<UserDto>
)
