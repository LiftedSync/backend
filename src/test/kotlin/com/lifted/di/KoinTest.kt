package com.lifted.di

import com.lifted.service.RoomService
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertSame

class KoinTest {

    @Test
    fun `appModule provides RoomService as singleton`() {
        val koin = koinApplication { modules(appModule) }.koin

        try {
            assertSame(koin.get<RoomService>(), koin.get<RoomService>())
        } finally {
            koin.close()
        }
    }
}
