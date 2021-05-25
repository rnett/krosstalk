package com.rnett.krosstalk.native_test

import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.client.krosstalkCall
import com.rnett.krosstalk.annotations.KrosstalkMethod
import com.rnett.krosstalk.Scope
import com.rnett.krosstalk.client.KrosstalkClient
import com.rnett.krosstalk.ktor.client.KtorClient
import com.rnett.krosstalk.ktor.client.KtorClientScope
import com.rnett.krosstalk.ktor.client.auth.KtorClientBasicAuth
import com.rnett.krosstalk.serialization.KotlinxBinarySerializationHandler
import com.rnett.krosstalk.toHeaders
import io.ktor.client.HttpClient
import io.ktor.client.features.HttpResponseValidator
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logging
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.cbor.Cbor

actual object MyKrosstalk : Krosstalk(), KrosstalkClient<KtorClientScope<*>> {
    actual override val serialization = KotlinxBinarySerializationHandler(Cbor { })
    override val serverUrl: String = "http://localhost:8087"

    override val client = KtorClient(
        baseClient = HttpClient().config {
            Logging {
                level = LogLevel.ALL
            }
        })

    actual object Auth : Scope, KtorClientBasicAuth()
}

actual suspend fun testBasic(id: Int, name: String): Item = krosstalkCall()

fun main() {
    try {
        runBlocking {
            println(testBasic(4, "test"))
        }
    } catch (e: Throwable){
        e.printStackTrace()
        throw e
    }
}