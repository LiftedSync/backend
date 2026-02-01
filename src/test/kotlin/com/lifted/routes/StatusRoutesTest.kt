package com.lifted.routes

import com.lifted.module
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlin.test.*

class StatusRoutesTest {

    @Test
    fun `health endpoint returns OK`() = testApplication {
        application {
            module()
        }

        val response = client.get("/health")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("OK", response.bodyAsText())
    }

    @Test
    fun `version endpoint returns version`() = testApplication {
        environment {
            config = MapApplicationConfig("app.version" to "1.2.3")
        }
        application {
            module()
        }

        val response = client.get("/version")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("1.2.3", response.bodyAsText())
    }
}
