package org.devstrike.plugins

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.devstrike.models.TicTacToeGame
import org.devstrike.socket

fun Application.configureRouting(game: TicTacToeGame) {
    routing {
        socket(game)
    }
}
