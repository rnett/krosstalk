package com.rnett.krosstalk.compose_test

import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.KrosstalkResult
import com.rnett.krosstalk.Scope
import com.rnett.krosstalk.ScopeInstance
import com.rnett.krosstalk.client.krosstalkCall
import com.rnett.krosstalk.ktor.client.KtorClient
import com.rnett.krosstalk.ktor.client.KtorKrosstalkClient
import com.rnett.krosstalk.ktor.client.auth.KtorClientBasicAuth
import com.rnett.krosstalk.ktor.client.auth.invoke
import com.rnett.krosstalk.serialization.KotlinxBinarySerializationHandler
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import kotlinx.serialization.cbor.Cbor

actual object TodoKrosstalk : Krosstalk(), KtorKrosstalkClient {
    actual override val serialization = KotlinxBinarySerializationHandler(Cbor { })

    override val client: KtorClient = KtorClient(HttpClient(Apache))
    override val serverUrl: String = "http://localhost:8082"

    actual object Auth : Scope, KtorClientBasicAuth()
}

typealias AuthCredentials = ScopeInstance<TodoKrosstalk.Auth>

actual suspend fun tryLogin(auth: ScopeInstance<TodoKrosstalk.Auth>): KrosstalkResult<Unit> = krosstalkCall()

suspend fun tryLogin(username: String, password: String): Boolean {
    return tryLogin(TodoKrosstalk.Auth(username, password)).isSuccess()
}

actual suspend fun getTodos(auth: ScopeInstance<TodoKrosstalk.Auth>): List<ToDo> = krosstalkCall()

actual suspend fun addTodo(
    message: String,
    auth: ScopeInstance<TodoKrosstalk.Auth>,
): List<ToDo> = krosstalkCall()

actual suspend fun removeTodo(
    idx: Int,
    auth: ScopeInstance<TodoKrosstalk.Auth>,
): List<ToDo> = krosstalkCall()