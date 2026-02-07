package com.lifted.routes

import com.lifted.models.toDto
import com.lifted.services.RoomManager
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.lifted.routes.AdminRoutes")

fun Route.adminRoutes() {
    authenticate("admin-basic") {
        route("/admin") {
            get("/rooms/count") {
                val count = RoomManager.getRoomCount()
                logger.debug("GET /admin/rooms/count — {}", count)
                call.respond(RoomCountResponse(count = count))
            }
            get("/rooms") {
                val rooms = RoomManager.getAllRooms().map { it.toDto() }
                logger.debug("GET /admin/rooms — {} room(s)", rooms.size)
                call.respond(rooms)
            }
        }
    }
}

@Serializable
data class RoomCountResponse(
    val count: Int
)