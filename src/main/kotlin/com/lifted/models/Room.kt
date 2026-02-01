package com.lifted.models

import com.lifted.dto.RoomDto
import com.lifted.dto.UserDto
import java.util.concurrent.ConcurrentHashMap

data class Room(
    val id: String,
    val platform: Platform,
    val hostId: String,
    val users: ConcurrentHashMap<String, User> = ConcurrentHashMap(),
    var currentState: VideoState = VideoState.PAUSED,
    var currentTime: Double = 0.0
)

fun Room.toDto() = RoomDto(
    id = id,
    platform = platform,
    hostId = hostId,
    currentState = currentState,
    currentTime = currentTime,
    users = users.values.map { UserDto(it.id, it.name) }
)