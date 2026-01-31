package com.lifted.routes

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.statusRoutes() {
    get("/health") {
        call.respondText("OK", ContentType.Text.Plain)
    }

    get("/version") {
        call.respondText("0.8.0", ContentType.Text.Plain)
    }
}