package org.devstrike.models

import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap


/*
* Game class that represents and contains the actual game and its logic
* */
class TicTacToeGame {

    //val that stores the game state
    private val state = MutableStateFlow(GameState())

    //a map to store a user (player) and its connection,
    // this is practically how we know the connection on which each user is connected to the server with which session
    //it is a concurrent hash map so that we can access this data from different threads
    //this data can be used to send specific data to specific clients
    private val playerSockets = ConcurrentHashMap<Char, WebSocketSession>()

    // coroutine scope that we can use to access various scopes from this class
    private val gameScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /*
    * Function to handle player connections to the server when the route is hit.
    * @param session: is the socket session with which the player connected to the server
    * User connects and we return a char of the connected player
    * */
    fun connectPlayer(session: WebSocketSession): Char? {
        //check if there is already a player X in the 'room' then make the connecting player 'O'
        // else, first player is always 'X'

        val isPlayerX = state.value.connectedPlayers.any{it == 'X'}
        val player = if(isPlayerX) 'O' else 'X'

        //then we update our game state
        state.update {
            //check if there is already an 'X' and 'O' player in the room then close the connection
            //this allows only two players to connect at a time
            if (state.value.connectedPlayers.contains(player)){
                return null
            }
            //check if the player has not connected before with a session (id),
            // then assign the connecting session as its session in the map
            if (!playerSockets.containsKey(player)){
                playerSockets[player] = session
            }
            //after that, update the game state with the existing players plus the new player
            it.copy(
                connectedPlayers = it.connectedPlayers +player
            )
        }
        return player
    }
}