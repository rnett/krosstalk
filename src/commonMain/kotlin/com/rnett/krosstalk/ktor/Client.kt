package com.rnett.krosstalk.ktor

import com.rnett.krosstalk.ClientHandler
import com.rnett.krosstalk.ClientScope

expect interface KtorClientScope: ClientScope

expect object KtorClient: ClientHandler<KtorClientScope>

expect class KtorClientAuth: KtorClientScope{
    val username: String
    val password: String
}