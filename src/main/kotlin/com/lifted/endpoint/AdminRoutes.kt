package com.lifted.endpoint

import com.lifted.data.dto.RoomCountDto
import com.lifted.data.model.toDto
import com.lifted.service.RoomService
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.lifted.endpoint.AdminRoutes")

fun Route.adminRoutes() {
    val roomService by inject<RoomService>()

    authenticate("admin-basic") {
        route("/admin") {
            get("/rooms/count") {
                val count = roomService.getRoomCount()
                logger.debug("GET /admin/rooms/count — {}", count)
                call.respond(RoomCountDto(count = count))
            }
            get("/rooms") {
                val rooms = roomService.getAllRooms().map { it.toDto() }
                logger.debug("GET /admin/rooms — {} room(s)", rooms.size)
                call.respond(rooms)
            }
        }
    }
}
