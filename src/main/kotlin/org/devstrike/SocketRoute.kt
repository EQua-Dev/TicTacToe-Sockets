/*
 * Copyright (c) 2023.
 * Richard Uzor
 * Under the authority of Devstrike Digital Limited
 */

package org.devstrike

import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.utils.io.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.json.Json
import org.devstrike.models.MakeTurn
import org.devstrike.models.TicTacToeGame

fun Route.socket(game: TicTacToeGame) {
    route("/play") {
        webSocket {
            val player = game.connectPlayer(this)

            if (player == null) {
                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Maximum of 2 players allowed"))
                return@webSocket
            }

            //if the player disconnects, the try block will skip, an exception will be thrown and then the 'finally' block will disconnect the player
            try {
                // Get the incoming data from the client connection
                incoming.consumeEach { frame ->
                    if (frame is Frame.Text) {
                        //extract the text if the data is a text
                        val action = extractAction(frame.readText())
                        game.finishTurn(player, action.x, action.y) //end the turn of the player, playing his move
                    }
                }
            } catch (e: Exception) {
                //print any exception
                e.printStackTrace()
            } finally {
                game.disconnectPlayer(player)
            }
        }
    }
}

/*
* Since all the requests from the client are to be in the same format, we have to get the value of the actual action requested by the client
* This Function receives the client request, and disintegrates its various parts into different variables
* @param message: is the value (in whole) gotten from the client
* */
private fun extractAction(message: String): MakeTurn {
    //syntax = make_turn#{...}
    val type = message.substringBefore("#")
    val body = message.substringAfter("#")
    return if (type == "make_turn") {
        Json.decodeFromString(body)
    } else MakeTurn(-1, -1)
}
