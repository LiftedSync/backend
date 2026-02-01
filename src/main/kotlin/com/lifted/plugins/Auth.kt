package com.lifted.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.lifted.plugins.Auth")

fun Application.configureAuth(): Boolean {
    val isDevelopment = environment.config.propertyOrNull("ktor.development")
        ?.getString()?.toBoolean() ?: false

    val adminUsername: String?
    val adminPassword: String?

    if (isDevelopment) {
        adminUsername = System.getenv("ADMIN_USERNAME")
            ?: environment.config.propertyOrNull("ktor.admin.username")?.getString()
        adminPassword = System.getenv("ADMIN_PASSWORD")
            ?: environment.config.propertyOrNull("ktor.admin.password")?.getString()
    } else {
        adminUsername = System.getenv("ADMIN_USERNAME")
        adminPassword = System.getenv("ADMIN_PASSWORD")
    }

    if (adminUsername == null || adminPassword == null) {
        logger.warn("Admin credentials not configured — admin endpoints disabled")
        return false
    }

    install(Authentication) {
        basic("admin-basic") {
            realm = "Admin"
            validate { credentials ->
                if (credentials.name == adminUsername && credentials.password == adminPassword) {
                    UserIdPrincipal(credentials.name)
                } else {
                    null
                }
            }
        }
    }
    return true
}