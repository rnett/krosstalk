package com.rnett.krosstalk.ktor

import com.rnett.krosstalk.ServerHandler
import com.rnett.krosstalk.ServerScope


actual object KtorServer : ServerHandler<KtorServerScope>
actual interface KtorServerScope : ServerScope
actual class KtorServerAuth actual constructor(actual val accounts: Map<String, String>) : KtorServerScope