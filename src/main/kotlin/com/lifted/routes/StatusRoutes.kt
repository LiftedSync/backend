package com.lifted.routes

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.statusRoutes() {
    get("/health") {
        call.respondText("OK", ContentType.Text.Plain)
    }

    get("/version") {
        val version = application.environment.config.propertyOrNull("app.version")?.getString() ?: "unknown"
        call.respondText(version, ContentType.Text.Plain)
    }
}