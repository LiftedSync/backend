package com.lifted.routes

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.lifted.routes.StatusRoutes")

fun Route.statusRoutes() {
    get("/health") {
        logger.debug("GET /health")
        call.respondText("OK", ContentType.Text.Plain)
    }

    get("/version") {
        val version = application.environment.config.propertyOrNull("app.version")?.getString() ?: "unknown"
        logger.debug("GET /version — {}", version)
        call.respondText(version, ContentType.Text.Plain)
    }
}