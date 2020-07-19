package com.rnett.krosstalk.ktor

import com.rnett.krosstalk.ClientHandler
import com.rnett.krosstalk.ClientScope


actual object KtorClient: ClientHandler<KtorClientScope>
actual interface KtorClientScope : ClientScope
actual class KtorClientAuth(actual val username: String, actual val password: String) : KtorClientScope