package com.rnett.krosstalk

import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*



actual interface ClientHandler<C: ClientScope>
actual interface ServerHandler<S: ServerScope>