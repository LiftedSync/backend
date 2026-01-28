package com.lifted.models

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

@Serializable
enum class VideoState {
    @SerialName("playing")
    PLAYING,
    @SerialName("paused")
    PAUSED
}
