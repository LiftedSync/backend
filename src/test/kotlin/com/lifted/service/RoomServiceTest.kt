package com.lifted.service

import com.lifted.data.model.Platform
import com.lifted.data.model.VideoState
import io.ktor.websocket.*
import io.mockk.mockk
import kotlin.test.*

class RoomServiceTest {

    private fun mockSession(): WebSocketSession = mockk(relaxed = true)

    private lateinit var roomService: RoomService
    private lateinit var hostId: String
    private lateinit var joinerId: String
    private lateinit var anotherId: String

    @BeforeTest
    fun setUp() {
        roomService = RoomService()
        val timestamp = System.nanoTime()
        hostId = "host-$timestamp"
        joinerId = "joiner-$timestamp"
        anotherId = "another-$timestamp"
    }

    @Test
    fun `createRoom creates room with valid ID`() {
        val room = roomService.createRoom(
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
        val room = roomService.createRoom(
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
    fun `createRoom with currentTime sets it on the room`() {
        val room = roomService.createRoom(
            hostId = hostId,
            hostName = "TestHost",
            platform = Platform.YOUTUBE,
            session = mockSession(),
            currentTime = 99.5
        )

        assertEquals(99.5, room.currentTime)
    }

    @Test
    fun `createRoom without currentTime defaults to zero`() {
        val room = roomService.createRoom(
            hostId = hostId,
            hostName = "TestHost",
            platform = Platform.YOUTUBE,
            session = mockSession()
        )

        assertEquals(0.0, room.currentTime)
    }

    @Test
    fun `joinRoom returns room for valid ID`() {
        val createdRoom = roomService.createRoom(
            hostId = hostId,
            hostName = "TestHost",
            platform = Platform.YOUTUBE,
            session = mockSession()
        )

        val joinedRoom = roomService.joinRoom(
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
        val room = roomService.joinRoom(
            roomId = "ZZZZ",
            userId = joinerId,
            userName = "TestUser",
            session = mockSession()
        )

        assertNull(room)
    }

    @Test
    fun `leaveRoom removes user`() {
        val room = roomService.createRoom(
            hostId = hostId,
            hostName = "TestHost",
            platform = Platform.YOUTUBE,
            session = mockSession()
        )

        roomService.joinRoom(room.id, joinerId, "Joiner", mockSession())
        assertEquals(2, room.users.size)

        val (leftRoom, leftUser) = assertNotNull(roomService.leaveRoom(joinerId))

        assertEquals("Joiner", leftUser.name)
        assertEquals(1, leftRoom.users.size)
        assertFalse(leftRoom.users.containsKey(joinerId))
    }

    @Test
    fun `leaveRoom deletes empty room`() {
        val room = roomService.createRoom(
            hostId = hostId,
            hostName = "TestHost",
            platform = Platform.YOUTUBE,
            session = mockSession()
        )
        val roomId = room.id

        assertNotNull(roomService.leaveRoom(hostId))

        // Room should be deleted - verify by trying to join
        val joinAttempt = roomService.joinRoom(
            roomId = roomId,
            userId = anotherId,
            userName = "Another",
            session = mockSession()
        )
        assertNull(joinAttempt, "Room should have been deleted")
    }

    @Test
    fun `updateState updates state`() {
        val room = roomService.createRoom(
            hostId = hostId,
            hostName = "TestHost",
            platform = Platform.YOUTUBE,
            session = mockSession()
        )

        assertEquals(VideoState.PAUSED, room.currentState)
        assertEquals(0.0, room.currentTime)

        roomService.updateState(hostId, VideoState.PLAYING, 42.5)

        val updatedRoom = roomService.getRoomForUser(hostId)
        assertNotNull(updatedRoom)
        assertEquals(VideoState.PLAYING, updatedRoom.currentState)
        assertEquals(42.5, updatedRoom.currentTime)
    }

    @Test
    fun `joinRoom ignores case of room ID`() {
        val room = roomService.createRoom(hostId, "TestHost", Platform.YOUTUBE, mockSession())

        val joinedRoom = roomService.joinRoom(room.id.lowercase(), joinerId, "Joiner", mockSession())

        assertNotNull(joinedRoom)
        assertEquals(room.id, joinedRoom.id)
    }

    @Test
    fun `leaveRoom keeps room while users remain`() {
        val room = roomService.createRoom(hostId, "TestHost", Platform.YOUTUBE, mockSession())
        roomService.joinRoom(room.id, joinerId, "Joiner", mockSession())

        roomService.leaveRoom(hostId)

        assertEquals(1, roomService.getRoomCount())
        assertEquals(room.id, roomService.getRoomForUser(joinerId)?.id)
    }

    @Test
    fun `leaveRoom returns null for user not in a room`() {
        assertNull(roomService.leaveRoom(anotherId))
    }

    @Test
    fun `updateState returns null for user not in a room`() {
        assertNull(roomService.updateState(anotherId, VideoState.PLAYING, 1.0))
    }

    @Test
    fun `navigate with matching URL resets playback to paused at zero`() {
        val room = roomService.createRoom(hostId, "TestHost", Platform.YOUTUBE, mockSession(), currentTime = 120.0)
        roomService.updateState(hostId, VideoState.PLAYING, 130.0)

        val result = roomService.navigate(hostId, "https://youtube.com/watch?v=new")

        assertIs<NavigateResult.Success>(result)
        assertEquals(room.id, result.room.id)
        assertEquals(VideoState.PAUSED, room.currentState)
        assertEquals(0.0, room.currentTime)
    }

    @Test
    fun `navigate with URL of another platform changes nothing`() {
        val room = roomService.createRoom(hostId, "TestHost", Platform.YOUTUBE, mockSession())
        roomService.updateState(hostId, VideoState.PLAYING, 30.0)

        val result = roomService.navigate(hostId, "https://netflix.com/watch/123")

        assertEquals(NavigateResult.InvalidUrl(Platform.YOUTUBE), result)
        assertEquals(VideoState.PLAYING, room.currentState)
        assertEquals(30.0, room.currentTime)
    }

    @Test
    fun `navigate returns NotInRoom for user not in a room`() {
        assertEquals(NavigateResult.NotInRoom, roomService.navigate(anotherId, "https://youtube.com/watch?v=x"))
    }

    @Test
    fun `leaveRoom removes user from the user index`() {
        val room = roomService.createRoom(hostId, "TestHost", Platform.YOUTUBE, mockSession())
        roomService.joinRoom(room.id, joinerId, "Joiner", mockSession())

        roomService.leaveRoom(joinerId)

        assertNull(roomService.getRoomForUser(joinerId))
    }

    @Test
    fun `getAllRooms and getRoomCount return all rooms`() {
        val first = roomService.createRoom(hostId, "Host1", Platform.YOUTUBE, mockSession())
        val second = roomService.createRoom(anotherId, "Host2", Platform.NETFLIX, mockSession())

        assertEquals(2, roomService.getRoomCount())
        assertEquals(setOf(first.id, second.id), roomService.getAllRooms().map { it.id }.toSet())
    }
}
