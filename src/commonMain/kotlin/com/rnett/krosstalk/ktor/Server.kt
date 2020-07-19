package com.rnett.krosstalk.ktor

import com.rnett.krosstalk.ServerHandler
import com.rnett.krosstalk.ServerScope

expect interface KtorServerScope: ServerScope

expect object KtorServer: ServerHandler<KtorServerScope>

expect class KtorServerAuth(accounts: Map<String, String>): KtorServerScope{
    val accounts: Map<String, String>
}