package com.rnett.krosstalk.client_test

import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.KrosstalkResult
import com.rnett.krosstalk.ScopeInstance
import com.rnett.krosstalk.annotations.EmptyBody
import com.rnett.krosstalk.annotations.ExplicitResult
import com.rnett.krosstalk.annotations.KrosstalkEndpoint
import com.rnett.krosstalk.annotations.KrosstalkMethod
import com.rnett.krosstalk.annotations.ServerURL
import com.rnett.krosstalk.client.KrosstalkClient
import com.rnett.krosstalk.client.krosstalkCall
import com.rnett.krosstalk.ktor.client.KtorClient
import com.rnett.krosstalk.ktor.client.KtorClientBasicAuth
import com.rnett.krosstalk.ktor.client.KtorClientScope
import com.rnett.krosstalk.serialization.KotlinxJsonObjectSerializationHandler
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json

@KrosstalkMethod(MyKrosstalk::class)
@KrosstalkEndpoint("/items", "GET")
@EmptyBody
suspend fun itemIds(): List<Int> = krosstalkCall()

@KrosstalkMethod(MyKrosstalk::class)
@KrosstalkEndpoint("/items/{id}", "GET")
@EmptyBody
@ExplicitResult
suspend fun getItem(id: Int): KrosstalkResult<Item> = krosstalkCall()

@KrosstalkMethod(MyKrosstalk::class)
@KrosstalkEndpoint("/user", "GET")
@EmptyBody
@ExplicitResult
suspend fun getUser(auth: ScopeInstance<MyKrosstalk.Auth>): KrosstalkResult<String> = krosstalkCall()

@KrosstalkMethod(MyKrosstalk::class)
@KrosstalkEndpoint("/test", "GET")
@EmptyBody
suspend fun getTestUnit(@ServerURL server: String): Unit = krosstalkCall()


object MyKrosstalk : Krosstalk(), KrosstalkClient<KtorClientScope<*>> {
    override val serialization = KotlinxJsonObjectSerializationHandler(Json { })
    override val client = KtorClient(HttpClient())
    override val serverUrl: String = "http://localhost:8080"

    object Auth : KtorClientBasicAuth()
}