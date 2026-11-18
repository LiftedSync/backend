package com.lifted.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Platform {
    @SerialName("youtube")
    YOUTUBE,
    @SerialName("crunchyroll")
    CRUNCHYROLL,
    @SerialName("netflix")
    NETFLIX,
    @SerialName("primevideo")
    PRIMEVIDEO
}

fun Platform.matchesUrl(url: String): Boolean = when (this) {
    Platform.YOUTUBE -> url.contains("youtube.com") || url.contains("youtu.be")
    Platform.CRUNCHYROLL -> url.contains("crunchyroll.com")
    Platform.NETFLIX -> url.contains("netflix.com")
    Platform.PRIMEVIDEO -> url.contains("primevideo.com") ||
        url.contains("amazon.com/gp/video") || url.contains("amazon.de/gp/video")
}
