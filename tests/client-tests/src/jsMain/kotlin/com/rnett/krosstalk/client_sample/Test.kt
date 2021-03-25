package com.rnett.krosstalk.client_sample

import com.rnett.krosstalk.*
import com.rnett.krosstalk.annotations.EmptyBody
import com.rnett.krosstalk.annotations.KrosstalkEndpoint
import com.rnett.krosstalk.annotations.KrosstalkMethod
import com.rnett.krosstalk.annotations.NullOn
import com.rnett.krosstalk.ktor.client.KtorClient
import com.rnett.krosstalk.ktor.client.KtorClientBasicAuth
import com.rnett.krosstalk.ktor.client.KtorClientScope
import com.rnett.krosstalk.serialization.KotlinxJsonObjectSerializationHandler
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@KrosstalkMethod(MyKrosstalk::class)
@KrosstalkEndpoint("/items", "GET")
@EmptyBody
suspend fun itemIds(): List<Int> = krosstalkCall()

@KrosstalkMethod(MyKrosstalk::class)
@KrosstalkEndpoint("/items/{id}", "GET")
@EmptyBody
@NullOn(404)
suspend fun getItem(id: Int): Item? = krosstalkCall()

fun main() {
    GlobalScope.launch {
        val items = itemIds()
        println("Items: $items")
        val item2 = getItem(items[1])
        println("Item 2: $item2")

        println("Item -2: ${getItem(-2)}")
    }
}

object MyKrosstalk : Krosstalk(), KrosstalkClient<KtorClientScope<*>> {
    override val serialization = KotlinxJsonObjectSerializationHandler(Json { })
    override val client = KtorClient("http://localhost:8080")
    val auth by scope(KtorClientBasicAuth())
}