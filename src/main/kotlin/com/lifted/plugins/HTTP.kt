package com.lifted.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*

fun Application.configureHTTP() {
    // Check development mode from configuration
    val isDevelopment = environment.config.propertyOrNull("ktor.development")
        ?.getString()?.toBoolean() ?: true

    install(CORS) {
        allowMethod(HttpMethod.Options)

        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)

        if (isDevelopment) {
            anyHost()
        } else {
            //Currently allow any Chrome extension to connect.
            allowOrigins { origin ->
                origin.startsWith("chrome-extension://")
            }
        }
    }
}