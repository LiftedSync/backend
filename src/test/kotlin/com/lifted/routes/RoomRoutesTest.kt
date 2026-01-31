package com.lifted.routes

import com.lifted.dto.*
import com.lifted.models.Platform
import com.lifted.models.VideoState
import com.lifted.module
import com.lifted.plugins.wsJson
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.server.testing.*
import io.ktor.websocket.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.test.*

class RoomRoutesTest {

    private fun websocketTest(block: suspend ApplicationTestBuilder.(HttpClient) -> Unit) = testApplication {
        application { module() }
        val client = createClient { install(WebSockets) }
        block(client)
    }

    @Test
    fun `create room returns room ID`() = websocketTest { client ->
        client.webSocket("/ws") {
            // Send create_room message
            val createMsg = """{"type":"create_room","userName":"Alice","platform":"youtube"}"""
            send(Frame.Text(createMsg))

            // Should receive room_created
            val frame1 = incoming.receive() as Frame.Text
            val response1 = wsJson.decodeFromString<RoomCreatedMessage>(frame1.readText())
            assertEquals("room_created", response1.type)
            assertEquals(4, response1.roomId.length)
            assertTrue(response1.roomId.all { it in 'A'..'Z' })

            // Should also receive room_joined
            val frame2 = incoming.receive() as Frame.Text
            val response2 = wsJson.decodeFromString<RoomJoinedMessage>(frame2.readText())
            assertEquals("room_joined", response2.type)
            assertEquals(response1.roomId, response2.roomId)
            assertEquals(Platform.YOUTUBE, response2.platform)
        }
    }

    @Test
    fun `create room with currentTime returns that time in room_joined`() = websocketTest { client ->
        client.webSocket("/ws") {
            val createMsg = """{"type":"create_room","userName":"Alice","platform":"youtube","currentTime":75.3}"""
            send(Frame.Text(createMsg))

            // room_created
            incoming.receive()

            // room_joined should have the specified currentTime
            val frame = incoming.receive() as Frame.Text
            val response = wsJson.decodeFromString<RoomJoinedMessage>(frame.readText())
            assertEquals("room_joined", response.type)
            assertEquals(75.3, response.currentTime)
        }
    }

    @Test
    fun `create room without currentTime defaults to zero in room_joined`() = websocketTest { client ->
        client.webSocket("/ws") {
            val createMsg = """{"type":"create_room","userName":"Alice","platform":"youtube"}"""
            send(Frame.Text(createMsg))

            // room_created
            incoming.receive()

            // room_joined should default to 0.0
            val frame = incoming.receive() as Frame.Text
            val response = wsJson.decodeFromString<RoomJoinedMessage>(frame.readText())
            assertEquals("room_joined", response.type)
            assertEquals(0.0, response.currentTime)
        }
    }

    @Test
    fun `join room receives room state`() = websocketTest { client ->
        val roomIdDeferred = CompletableDeferred<String>()

        coroutineScope {
            // Host creates room and stays connected
            val hostJob = launch {
                client.webSocket("/ws") {
                    send(Frame.Text("""{"type":"create_room","userName":"Host","platform":"youtube"}"""))
                    val frame = incoming.receive() as Frame.Text
                    val response = wsJson.decodeFromString<RoomCreatedMessage>(frame.readText())
                    roomIdDeferred.complete(response.roomId)
                    // Consume room_joined message
                    incoming.receive()
                    // Wait for guest to join (receive user_joined notification)
                    incoming.receive()
                    // Stay connected briefly
                    delay(100)
                }
            }

            // Another user joins after room is created
            val guestJob = launch {
                val roomId = roomIdDeferred.await()
                client.webSocket("/ws") {
                    send(Frame.Text("""{"type":"join_room","roomId":"$roomId","userName":"Guest"}"""))

                    val frame = incoming.receive() as Frame.Text
                    val response = wsJson.decodeFromString<RoomJoinedMessage>(frame.readText())

                    assertEquals("room_joined", response.type)
                    assertEquals(roomId, response.roomId)
                    assertEquals(Platform.YOUTUBE, response.platform)
                    assertEquals(VideoState.PAUSED, response.state)
                    assertEquals(0.0, response.currentTime)
                    assertEquals(2, response.users.size)
                }
            }

            hostJob.join()
            guestJob.join()
        }
    }

    @Test
    fun `video update broadcasts to others`() = websocketTest { client ->
        val roomIdDeferred = CompletableDeferred<String>()
        val guestJoined = CompletableDeferred<Unit>()

        coroutineScope {
            // Host creates room, waits for guest, then sends video update
            val hostJob = launch {
                client.webSocket("/ws") {
                    send(Frame.Text("""{"type":"create_room","userName":"Host","platform":"youtube"}"""))
                    val frame = incoming.receive() as Frame.Text
                    val response = wsJson.decodeFromString<RoomCreatedMessage>(frame.readText())
                    roomIdDeferred.complete(response.roomId)
                    // Consume room_joined
                    incoming.receive()
                    // Wait for guest to join (user_joined notification)
                    incoming.receive()
                    guestJoined.complete(Unit)
                    // Send video update
                    send(Frame.Text("""{"type":"video_update","state":"playing","currentTime":30.0}"""))
                    // Stay connected briefly
                    delay(100)
                }
            }

            // Guest joins and waits for sync
            val guestJob = launch {
                val roomId = roomIdDeferred.await()
                client.webSocket("/ws") {
                    send(Frame.Text("""{"type":"join_room","roomId":"$roomId","userName":"Guest"}"""))
                    // Receive room_joined
                    incoming.receive()
                    // Wait until host knows we joined
                    guestJoined.await()
                    // Wait for sync_update from host
                    withTimeout(2000) {
                        val frame = incoming.receive() as Frame.Text
                        val response = wsJson.decodeFromString<SyncUpdateMessage>(frame.readText())

                        assertEquals("sync_update", response.type)
                        assertEquals(VideoState.PLAYING, response.state)
                        assertEquals(30.0, response.currentTime)
                    }
                }
            }

            hostJob.join()
            guestJob.join()
        }
    }

    @Test
    fun `join invalid room returns error`() = websocketTest { client ->
        client.webSocket("/ws") {
            // Try to join non-existent room
            send(Frame.Text("""{"type":"join_room","roomId":"ZZZZ","userName":"Guest"}"""))

            val frame = incoming.receive() as Frame.Text
            val response = wsJson.decodeFromString<ErrorMessage>(frame.readText())

            assertEquals("error", response.type)
            assertEquals("ROOM_NOT_FOUND", response.code)
            assertTrue(response.message.contains("ZZZZ"))
        }
    }

    @Test
    fun `disconnect cleans up user`() = websocketTest { client ->
        val roomIdDeferred = CompletableDeferred<String>()
        val guestDisconnected = CompletableDeferred<Unit>()
        val testComplete = CompletableDeferred<Unit>()

        coroutineScope {
            // Host creates room and stays connected throughout the test
            val hostJob = launch {
                client.webSocket("/ws") {
                    send(Frame.Text("""{"type":"create_room","userName":"Host","platform":"youtube"}"""))
                    val frame = incoming.receive() as Frame.Text
                    val response = wsJson.decodeFromString<RoomCreatedMessage>(frame.readText())
                    roomIdDeferred.complete(response.roomId)
                    // Consume room_joined
                    incoming.receive()
                    // Wait for guest to join (user_joined)
                    incoming.receive()
                    // Wait for guest to disconnect (user_left)
                    incoming.receive()
                    guestDisconnected.complete(Unit)
                    // Wait for new guest to join (user_joined)
                    incoming.receive()
                    // Stay connected until test completes
                    testComplete.await()
                }
            }

            // Guest joins then disconnects
            val guestJob = launch {
                val roomId = roomIdDeferred.await()
                client.webSocket("/ws") {
                    send(Frame.Text("""{"type":"join_room","roomId":"$roomId","userName":"Guest"}"""))
                    incoming.receive() // room_joined
                    // Disconnect by ending the block
                }
            }
            guestJob.join()

            // Wait for host to receive user_left
            guestDisconnected.await()

            // NewGuest joins - should not see disconnected Guest
            val newGuestJob = launch {
                val roomId = roomIdDeferred.await()
                client.webSocket("/ws") {
                    send(Frame.Text("""{"type":"join_room","roomId":"$roomId","userName":"NewGuest"}"""))
                    val frame = incoming.receive() as Frame.Text
                    val response = wsJson.decodeFromString<RoomJoinedMessage>(frame.readText())

                    // Should have 2 users: host + new guest (disconnected guest should be gone)
                    assertEquals(2, response.users.size)
                    assertTrue(response.users.any { it.name == "Host" })
                    assertTrue(response.users.any { it.name == "NewGuest" })
                    assertFalse(response.users.any { it.name == "Guest" })
                }
            }
            newGuestJob.join()

            testComplete.complete(Unit)
            hostJob.join()
        }
    }
}
