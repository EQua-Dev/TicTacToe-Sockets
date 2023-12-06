package org.devstrike

import io.ktor.server.application.*
import org.devstrike.models.TicTacToeGame
import org.devstrike.plugins.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {

    val game = TicTacToeGame()
    configureSockets()
    configureSerialization()
    configureMonitoring()
    configureRouting(game)
}
