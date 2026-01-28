package com.lifted.dto

import com.lifted.models.Platform
import com.lifted.models.VideoState
import kotlin.test.*

class MessageParserTest {

    @Test
    fun `parse CreateRoomMessage`() {
        val json = """{"type":"create_room","userName":"Alice","platform":"youtube"}"""

        val result = MessageParser.parseClientMessage(json)

        assertIs<CreateRoomMessage>(result)
        assertEquals("Alice", result.userName)
        assertEquals(Platform.YOUTUBE, result.platform)
    }

    @Test
    fun `parse JoinRoomMessage`() {
        val json = """{"type":"join_room","roomId":"ABCD","userName":"Bob"}"""

        val result = MessageParser.parseClientMessage(json)

        assertIs<JoinRoomMessage>(result)
        assertEquals("ABCD", result.roomId)
        assertEquals("Bob", result.userName)
    }

    @Test
    fun `parse VideoUpdateMessage`() {
        val json = """{"type":"video_update","state":"playing","currentTime":123.45}"""

        val result = MessageParser.parseClientMessage(json)

        assertIs<VideoUpdateMessage>(result)
        assertEquals(VideoState.PLAYING, result.state)
        assertEquals(123.45, result.currentTime)
    }

    @Test
    fun `parse NavigateMessage`() {
        val json = """{"type":"navigate","url":"https://youtube.com/watch?v=abc123"}"""

        val result = MessageParser.parseClientMessage(json)

        assertIs<NavigateMessage>(result)
        assertEquals("https://youtube.com/watch?v=abc123", result.url)
    }

    @Test
    fun `parse HeartbeatMessage`() {
        val json = """{"type":"heartbeat"}"""

        val result = MessageParser.parseClientMessage(json)

        assertIs<HeartbeatMessage>(result)
    }

    @Test
    fun `parse LeaveRoomMessage`() {
        val json = """{"type":"leave_room"}"""

        val result = MessageParser.parseClientMessage(json)

        assertIs<LeaveRoomMessage>(result)
    }

    @Test
    fun `parse invalid JSON returns null`() {
        val invalidJson = """not valid json at all"""

        val result = MessageParser.parseClientMessage(invalidJson)

        assertNull(result)
    }

    @Test
    fun `parse unknown type returns null`() {
        val json = """{"type":"unknown_type","data":"test"}"""

        val result = MessageParser.parseClientMessage(json)

        assertNull(result)
    }

    @Test
    fun `parse missing type returns null`() {
        val json = """{"userName":"Alice","platform":"youtube"}"""

        val result = MessageParser.parseClientMessage(json)

        assertNull(result)
    }
}