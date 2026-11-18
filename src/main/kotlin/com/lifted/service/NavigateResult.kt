package com.lifted.service

import com.lifted.data.model.Platform
import com.lifted.data.model.Room

sealed interface NavigateResult {
    data class Success(val room: Room) : NavigateResult

    data class InvalidUrl(val platform: Platform) : NavigateResult

    data object NotInRoom : NavigateResult
}
