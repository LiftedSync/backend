package com.lifted.services

import com.lifted.models.Platform
import com.lifted.models.VideoState
import io.ktor.websocket.*
import io.mockk.mockk
import kotlin.test.*

class RoomManagerTest {

    private fun mockSession(): WebSocketSession = mockk(relaxed = true)

    private lateinit var hostId: String
    private lateinit var joinerId: String
    private lateinit var anotherId: String

    @BeforeTest
    fun setUp() {
        val timestamp = System.nanoTime()
        hostId = "host-$timestamp"
        joinerId = "joiner-$timestamp"
        anotherId = "another-$timestamp"
    }

    @AfterTest
    fun tearDown() {
        RoomManager.leaveRoom(hostId)
        RoomManager.leaveRoom(joinerId)
        RoomManager.leaveRoom(anotherId)
    }

    @Test
    fun `createRoom creates room with valid ID`() {
        val room = RoomManager.createRoom(
            hostId = hostId,
            hostName = "TestHost",
            platform = Platform.YOUTUBE,
            session = mockSession()
        )

        assertNotNull(room)
        assertEquals(4, room.id.length)
        assertTrue(room.id.all { it in 'A'..'Z' }, "Room ID should be uppercase letters only")
    }

    @Test
    fun `createRoom adds host to room`() {
        val room = RoomManager.createRoom(
            hostId = hostId,
            hostName = "TestHost",
            platform = Platform.YOUTUBE,
            session = mockSession()
        )

        assertEquals(1, room.users.size)
        assertTrue(room.users.containsKey(hostId))
        assertEquals("TestHost", room.users[hostId]?.name)
    }

    @Test
    fun `joinRoom returns room for valid ID`() {
        val createdRoom = RoomManager.createRoom(
            hostId = hostId,
            hostName = "TestHost",
            platform = Platform.YOUTUBE,
            session = mockSession()
        )

        val joinedRoom = RoomManager.joinRoom(
            roomId = createdRoom.id,
            userId = joinerId,
            userName = "Joiner",
            session = mockSession()
        )

        assertNotNull(joinedRoom)
        assertEquals(createdRoom.id, joinedRoom.id)
        assertEquals(2, joinedRoom.users.size)
    }

    @Test
    fun `joinRoom returns null for non-existent room`() {
        val room = RoomManager.joinRoom(
            roomId = "ZZZZ",
            userId = joinerId,
            userName = "TestUser",
            session = mockSession()
        )

        assertNull(room)
    }

    @Test
    fun `leaveRoom removes user`() {
        val room = RoomManager.createRoom(
            hostId = hostId,
            hostName = "TestHost",
            platform = Platform.YOUTUBE,
            session = mockSession()
        )

        RoomManager.joinRoom(room.id, joinerId, "Joiner", mockSession())
        assertEquals(2, room.users.size)

        val (leftRoom, leftUser) = RoomManager.leaveRoom(joinerId)

        assertNotNull(leftRoom)
        assertNotNull(leftUser)
        assertEquals("Joiner", leftUser.name)
        assertEquals(1, leftRoom.users.size)
        assertFalse(leftRoom.users.containsKey(joinerId))
    }

    @Test
    fun `leaveRoom deletes empty room`() {
        val room = RoomManager.createRoom(
            hostId = hostId,
            hostName = "TestHost",
            platform = Platform.YOUTUBE,
            session = mockSession()
        )
        val roomId = room.id

        val (leftRoom, leftUser) = RoomManager.leaveRoom(hostId)

        assertNotNull(leftRoom)
        assertNotNull(leftUser)

        // Room should be deleted - verify by trying to join
        val joinAttempt = RoomManager.joinRoom(
            roomId = roomId,
            userId = anotherId,
            userName = "Another",
            session = mockSession()
        )
        assertNull(joinAttempt, "Room should have been deleted")
    }

    @Test
    fun `updateRoomState updates state`() {
        val room = RoomManager.createRoom(
            hostId = hostId,
            hostName = "TestHost",
            platform = Platform.YOUTUBE,
            session = mockSession()
        )

        assertEquals(VideoState.PAUSED, room.currentState)
        assertEquals(0.0, room.currentTime)

        RoomManager.updateRoomState(room.id, VideoState.PLAYING, 42.5)

        val updatedRoom = RoomManager.getRoomForUser(hostId)
        assertNotNull(updatedRoom)
        assertEquals(VideoState.PLAYING, updatedRoom.currentState)
        assertEquals(42.5, updatedRoom.currentTime)
    }
}