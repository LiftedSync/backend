package com.lifted.models

import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap

data class Room(
    val id: String,
    val platform: Platform,
    val hostId: String,
    val users: ConcurrentHashMap<String, User> = ConcurrentHashMap(),
    var currentState: VideoState = VideoState.PAUSED,
    var currentTime: Double = 0.0
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

fun Room.toDto() = RoomDto(
    id = id,
    platform = platform,
    hostId = hostId,
    currentState = currentState,
    currentTime = currentTime,
    users = users.values.map {it.toDto() }
)