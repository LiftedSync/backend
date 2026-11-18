package com.lifted.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class VideoState {
    @SerialName("playing")
    PLAYING,
    @SerialName("paused")
    PAUSED
}
