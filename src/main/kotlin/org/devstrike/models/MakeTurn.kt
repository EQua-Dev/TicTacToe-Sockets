package org.devstrike.models

/*
* Data class to enable the client tell the server where (the position) the player made their turn
* */
@kotlinx.serialization.Serializable
data class MakeTurn(val x: Int, val y: Int)
