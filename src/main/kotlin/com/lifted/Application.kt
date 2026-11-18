package com.lifted

import com.lifted.di.configureKoin
import com.lifted.plugin.configureAuth
import com.lifted.plugin.configureHTTP
import com.lifted.plugin.configureSerialization
import com.lifted.plugin.configureSockets
import com.lifted.endpoint.adminRoutes
import com.lifted.endpoint.roomRoutes
import com.lifted.endpoint.statusRoutes
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureKoin()
    configureSockets()
    configureHTTP()
    configureSerialization()
    val adminEnabled = configureAuth()

    routing {
        statusRoutes()
        roomRoutes()
        if (adminEnabled) {
            adminRoutes()
        }
    }
}
