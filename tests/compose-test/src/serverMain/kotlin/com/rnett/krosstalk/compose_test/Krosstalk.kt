package com.rnett.krosstalk.compose_test

import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.Scope
import com.rnett.krosstalk.ScopeInstance
import com.rnett.krosstalk.ktor.server.KtorKrosstalkServer
import com.rnett.krosstalk.ktor.server.KtorServer
import com.rnett.krosstalk.ktor.server.auth.KtorServerBasicAuth
import com.rnett.krosstalk.result.KrosstalkResult
import com.rnett.krosstalk.result.runKrosstalkCatching
import com.rnett.krosstalk.serialization.KotlinxBinarySerializationHandler
import com.rnett.krosstalk.server.value
import io.ktor.auth.BasicAuthenticationProvider
import io.ktor.auth.Principal
import kotlinx.datetime.Clock
import kotlinx.serialization.cbor.Cbor

data class User(val username: String) : Principal

actual object TodoKrosstalk : Krosstalk(), KtorKrosstalkServer {
    actual override val serialization = KotlinxBinarySerializationHandler(Cbor { })

    override val server: KtorServer = KtorServer

    actual object Auth : Scope, KtorServerBasicAuth<User>() {
        override fun BasicAuthenticationProvider.Configuration.configure() {
            validate {
                if (it.name == it.password)
                    User(it.name)
                else
                    null
            }
        }
    }
}

actual suspend fun tryLogin(auth: ScopeInstance<TodoKrosstalk.Auth>): KrosstalkResult<Unit> =
    runKrosstalkCatching { Unit }

val todos: MutableMap<User, List<ToDo>> = mutableMapOf()

actual suspend fun getTodos(auth: ScopeInstance<TodoKrosstalk.Auth>): List<ToDo> {
    return todos.getOrPut(auth.value) { emptyList() }
}

actual suspend fun addTodo(
    message: String,
    auth: ScopeInstance<TodoKrosstalk.Auth>,
): List<ToDo> {
    val list = todos.getOrElse(auth.value) { emptyList() } + ToDo(Clock.System.now(), message)
    todos[auth.value] = list
    return list
}

actual suspend fun removeTodo(
    idx: Int,
    auth: ScopeInstance<TodoKrosstalk.Auth>,
): List<ToDo> {
    val list = todos.getOrElse(auth.value) { emptyList() }
    if (idx > list.lastIndex || idx < 0)
        return list

    val newList = list.toMutableList().apply { removeAt(idx) }
    todos[auth.value] = newList
    return newList
}