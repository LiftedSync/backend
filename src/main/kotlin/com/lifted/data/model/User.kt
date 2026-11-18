package com.lifted.data.model

import com.lifted.data.dto.UserDto
import io.ktor.websocket.*

data class User(
    val id: String,
    val name: String,
    val session: WebSocketSession
)

fun User.toDto() = UserDto(
    id = id,
    name = name
)
