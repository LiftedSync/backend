package com.lifted.models

import io.ktor.websocket.*

data class User(
    val id: String,
    val name: String,
    val session: WebSocketSession
)
