package com.lifted

import com.lifted.plugins.configureHTTP
import com.lifted.plugins.configureSerialization
import com.lifted.plugins.configureSockets
import com.lifted.routes.roomRoutes
import com.lifted.routes.statusRoutes
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureSockets()
    configureHTTP()
    configureSerialization()

    routing {
        statusRoutes()
        roomRoutes()
    }
}
