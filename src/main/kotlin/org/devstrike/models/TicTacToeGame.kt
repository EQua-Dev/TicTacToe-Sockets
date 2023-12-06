package org.devstrike.models

import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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

    // handler to for delaying the restart of the game
    private var delayGameJob: Job? = null

    //variables to store the players' scores
    private var XScore = state.value.XScore
    private var OScore = state.value.OScore


    //in the initialization of this class, everytime the game state changes, we want to call the broadcast function, passing the state to it
    init {
        state.onEach { ::broadcast}.launchIn(gameScope)
    }

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

    /*
    * Function that handles player disconnections from the server when the route is hit.
    * @param player: is the player to be disconnected
    * */
    fun disconnectPlayer(player: Char) {
        //we remove the player from the connected players map
        playerSockets.remove(player)
        //then we update the game state, removing the player from the object
        state.update {
            it.copy(
                connectedPlayers = it.connectedPlayers - player
            )
        }
    }

    /*
    * Function to send messages to our connected players (in this case, the game state)
    * @param state: this is the state of the game which can change
    * */
    suspend fun broadcast(state: GameState){
        //we get all the players in our 'room' and send them the game state object as a JSON object
        playerSockets.values.forEach{ socket ->
            socket.send(
                Json.encodeToString(state)
            )

        }
    }

    fun finishTurn(player: Char, x: Int, y: Int){
        //check if the selected field to play is not empty  or if there is already a winner
        //then, do not allow the requesting player to play
        if (state.value.field[y][x] != null || state.value.winningPlayer != null){
            return
        }
        //check if the requesting player is not the player to play
        //then, do not allow the requesting player to play
        if (state.value.playerAtTurn != player){
            return
        }
        val currentPlayer = state.value.playerAtTurn

        //we update the game state
        state.update {
            // populate the position of the selected cell with the character of the current player
            val newField = it.field.also { field ->
                field[y][x] = currentPlayer
            }
            // the board is full if all cells (elements) of the board (new board) is not empty
            val isBoardFull = newField.all { it.all { it != null } }
            if (isBoardFull){
                // restart the game
                // TODO: 06/12/2023 save the scores fo each user before restart
                when(getWinningPlayer()){
                    'X' -> XScore += 1
                    'O' -> OScore += 1
                    null -> {
                        XScore += 0
                        OScore += 0
                    }
                }
                startNewRoundDelayed()
            }
            // update the state of the game with the updated values
            it.copy(
                playerAtTurn = if (currentPlayer == 'X') 'O' else 'X',
                field =  newField,
                isBoardFull = isBoardFull,
                winningPlayer =  getWinningPlayer()?.also{
                    startNewRoundDelayed()
                }
            )
        }
    }

    /*
    * Function to handle the logic that checks the winner of the game (if any)
    * */
    private fun getWinningPlayer(): Char? {
        val field = state.value.field
        return if (field[0][0] != null && field[0][0] == field[0][1] && field[0][1] == field[0][2]) {
            field[0][0]
        } else if (field[1][0] != null && field[1][0] == field[1][1] && field[1][1] == field[1][2]) {
            field[1][0]
        } else if (field[2][0] != null && field[2][0] == field[2][1] && field[2][1] == field[2][2]) {
            field[2][0]
        } else if (field[0][0] != null && field[0][0] == field[1][0] && field[1][0] == field[2][0]) {
            field[0][0]
        } else if (field[0][1] != null && field[0][1] == field[1][1] && field[1][1] == field[2][1]) {
            field[0][1]
        } else if (field[0][2] != null && field[0][2] == field[1][2] && field[1][2] == field[2][2]) {
            field[0][2]
        } else if (field[0][0] != null && field[0][0] == field[1][1] && field[1][1] == field[2][2]) {
            field[0][0]
        } else if (field[0][2] != null && field[0][2] == field[1][1] && field[1][1] == field[2][0]) {
            field[0][2]
        } else null
    }

    /*
    * Function to delay for some time and then reset the game board
    * */
    private fun startNewRoundDelayed() {
        delayGameJob?.cancel() //if there is an ongoing job, cancel it
        delayGameJob = gameScope.launch {
            delay(5000L)
            state.update {
                //reset the game after the delay
                it.copy(
                    playerAtTurn = 'X',
                    field = GameState.emptyField(),
                    winningPlayer = null,
                    isBoardFull = false,
                    XScore = XScore,
                    OScore = OScore
                )
            }
        }
    }
}