package com.lifted.routes

import com.lifted.dto.RoomCountResponse
import com.lifted.dto.RoomDto
import com.lifted.models.Platform
import com.lifted.models.VideoState
import com.lifted.module
import com.lifted.services.RoomManager
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlin.test.*

class AdminRoutesTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val testUsers = mutableListOf<String>()

    private val adminConfig = MapApplicationConfig(
        "ktor.admin.username" to "admin",
        "ktor.admin.password" to "admin",
        "ktor.development" to "true"
    )

    @AfterTest
    fun tearDown() {
        testUsers.forEach { RoomManager.leaveRoom(it) }
        testUsers.clear()
    }

    @Test
    fun `rooms count returns zero when no rooms`() = testApplication {
        environment { config = adminConfig }
        application { module() }

        val response = client.get("/admin/rooms/count") {
            basicAuth("admin", "admin")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.decodeFromString<RoomCountResponse>(response.bodyAsText())
        assertEquals(0, body.count)
    }

    @Test
    fun `rooms count returns correct count after creating rooms`() = testApplication {
        environment { config = adminConfig }
        application { module() }

        val host1 = "admin-test-host1-${System.nanoTime()}"
        val host2 = "admin-test-host2-${System.nanoTime()}"
        testUsers.addAll(listOf(host1, host2))

        RoomManager.createRoom(host1, "Host1", Platform.YOUTUBE, mockk(relaxed = true))
        RoomManager.createRoom(host2, "Host2", Platform.NETFLIX, mockk(relaxed = true))

        val response = client.get("/admin/rooms/count") {
            basicAuth("admin", "admin")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.decodeFromString<RoomCountResponse>(response.bodyAsText())
        assertEquals(2, body.count)
    }

    @Test
    fun `rooms list returns empty list when no rooms`() = testApplication {
        environment { config = adminConfig }
        application { module() }

        val response = client.get("/admin/rooms") {
            basicAuth("admin", "admin")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.decodeFromString<List<RoomDto>>(response.bodyAsText())
        assertTrue(body.isEmpty())
    }

    @Test
    fun `rooms list returns room details`() = testApplication {
        environment { config = adminConfig }
        application { module() }

        val hostId = "admin-test-host-${System.nanoTime()}"
        val joinerId = "admin-test-joiner-${System.nanoTime()}"
        testUsers.addAll(listOf(hostId, joinerId))

        val room = RoomManager.createRoom(hostId, "Alice", Platform.YOUTUBE, mockk(relaxed = true), 45.0)
        RoomManager.joinRoom(room.id, joinerId, "Bob", mockk(relaxed = true))
        RoomManager.updateRoomState(room.id, VideoState.PLAYING, 50.0)

        val response = client.get("/admin/rooms") {
            basicAuth("admin", "admin")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val rooms = json.decodeFromString<List<RoomDto>>(response.bodyAsText())
        assertEquals(1, rooms.size)

        val dto = rooms[0]
        assertEquals(room.id, dto.id)
        assertEquals(Platform.YOUTUBE, dto.platform)
        assertEquals(hostId, dto.hostId)
        assertEquals(VideoState.PLAYING, dto.currentState)
        assertEquals(50.0, dto.currentTime)
        assertEquals(2, dto.users.size)
        assertTrue(dto.users.any { it.name == "Alice" })
        assertTrue(dto.users.any { it.name == "Bob" })
    }

    @Test
    fun `returns 401 when no credentials provided`() = testApplication {
        environment { config = adminConfig }
        application { module() }

        val response = client.get("/admin/rooms/count")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `returns 401 when wrong credentials provided`() = testApplication {
        environment { config = adminConfig }
        application { module() }

        val response = client.get("/admin/rooms/count") {
            basicAuth("test", "test")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `returns 401 for rooms list without credentials`() = testApplication {
        environment { config = adminConfig }
        application { module() }

        val response = client.get("/admin/rooms")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `admin endpoints disabled when credentials not configured in production`() = testApplication {
        environment { config = MapApplicationConfig("ktor.development" to "false") }
        application { module() }

        val response = client.get("/admin/rooms/count") {
            basicAuth("admin", "admin")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}