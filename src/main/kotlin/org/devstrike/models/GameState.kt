package org.devstrike.models

/*
* Represents the current state of the game which will be updated and sent to the client side as JSON
* This data class contains what the client will need to know about the current state of the game
* */
@kotlinx.serialization.Serializable //we want to be able to parse this into JSON
data class GameState(
    val playerAtTurn: Char? = 'X', //who is the player to play next...initialized to make X always start
    val field: Array<Array<Char?>> = emptyField(), //two dimensional array to represent what our fields look like
    val winningPlayer: Char? = null, //who is the winning player
    val isBoardFull: Boolean = false, //if the board is full then it is a draw and game should restart
    val connectedPlayers: List<Char> = emptyList() //list of connected users, which will determine whether the game should start
){
    companion object{
        fun emptyField(): Array<Array<Char?>>{
            //this is an instance of how our game field will look like
            return arrayOf(
                arrayOf(null, null, null),
                arrayOf(null, null, null),
                arrayOf(null, null, null),

            )
        }
    }

    //auto generated functions to ensure that if we compare two game objects, we will get the same behaviour
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GameState

        if (playerAtTurn != other.playerAtTurn) return false
        if (!field.contentDeepEquals(other.field)) return false
        if (winningPlayer != other.winningPlayer) return false
        if (isBoardFull != other.isBoardFull) return false
        if (connectedPlayers != other.connectedPlayers) return false

        return true
    }

    override fun hashCode(): Int {
        var result = playerAtTurn?.hashCode() ?: 0
        result = 31 * result + field.contentDeepHashCode()
        result = 31 * result + (winningPlayer?.hashCode() ?: 0)
        result = 31 * result + isBoardFull.hashCode()
        result = 31 * result + connectedPlayers.hashCode()
        return result
    }
}
