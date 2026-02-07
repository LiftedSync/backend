package com.lifted.models

import io.ktor.websocket.*
import kotlinx.serialization.Serializable

data class User(
    val id: String,
    val name: String,
    val session: WebSocketSession
)

@Serializable
data class UserDto(
    val id: String,
    val name: String
)

fun User.toDto() = UserDto(
    id = id,
    name = name
)