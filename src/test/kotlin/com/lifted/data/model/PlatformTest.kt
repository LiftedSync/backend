package com.lifted.data.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlatformTest {

    @Test
    fun `youtube matches youtube_com and youtu_be`() {
        assertTrue(Platform.YOUTUBE.matchesUrl("https://www.youtube.com/watch?v=abc"))
        assertTrue(Platform.YOUTUBE.matchesUrl("https://youtu.be/abc"))
        assertFalse(Platform.YOUTUBE.matchesUrl("https://www.netflix.com/watch/1"))
    }

    @Test
    fun `crunchyroll matches crunchyroll_com`() {
        assertTrue(Platform.CRUNCHYROLL.matchesUrl("https://www.crunchyroll.com/watch/X"))
        assertFalse(Platform.CRUNCHYROLL.matchesUrl("https://youtu.be/abc"))
    }

    @Test
    fun `netflix matches netflix_com`() {
        assertTrue(Platform.NETFLIX.matchesUrl("https://www.netflix.com/watch/1"))
        assertFalse(Platform.NETFLIX.matchesUrl("https://www.primevideo.com/detail/1"))
    }

    @Test
    fun `prime video matches primevideo_com and amazon video paths`() {
        assertTrue(Platform.PRIMEVIDEO.matchesUrl("https://www.primevideo.com/detail/1"))
        assertTrue(Platform.PRIMEVIDEO.matchesUrl("https://www.amazon.com/gp/video/detail/1"))
        assertTrue(Platform.PRIMEVIDEO.matchesUrl("https://www.amazon.de/gp/video/detail/1"))
        assertFalse(Platform.PRIMEVIDEO.matchesUrl("https://www.amazon.de/dp/B000"))
    }
}
