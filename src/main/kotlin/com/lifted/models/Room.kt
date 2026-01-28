package com.lifted.models

import java.util.concurrent.ConcurrentHashMap

data class Room(
    val id: String,
    val platform: Platform,
    val hostId: String,
    val users: ConcurrentHashMap<String, User> = ConcurrentHashMap(),
    var currentState: VideoState = VideoState.PAUSED,
    var currentTime: Double = 0.0
)
