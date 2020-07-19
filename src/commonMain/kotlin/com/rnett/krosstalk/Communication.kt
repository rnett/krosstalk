package com.rnett.krosstalk

import kotlinx.serialization.Serializable

expect interface ClientHandler<C: ClientScope>
expect interface ServerHandler<S: ServerScope>


@Serializable
data class KrosstalkCall(val function: String, val parameters: Map<String, ByteArray>)