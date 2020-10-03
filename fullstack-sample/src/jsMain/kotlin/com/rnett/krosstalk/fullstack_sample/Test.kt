package com.rnett.krosstalk.fullstack_sample

import com.rnett.krosstalk.*
import com.rnett.krosstalk.ktor.client.BasicCredentials
import com.rnett.krosstalk.ktor.client.KtorClient
import com.rnett.krosstalk.ktor.client.KtorClientBasicAuth
import com.rnett.krosstalk.ktor.client.KtorClientScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.cbor.Cbor

actual suspend fun doThing(data: Data): List<String> = krosstalkCall()

actual suspend fun doAuthThing(num: Int): Data = krosstalkCall()

actual suspend fun Int.doExt(other: Int): Double = krosstalkCall()


//TODO test instance receiver (should work)
fun main() {
    GlobalScope.launch {
        console.log("Testing doThing")
        println(doThing(Data(3, "test")))
        console.log("Testing doAuthThing, expecting error")
        try {
            println(doAuthThing(2))
        } catch (e: Exception) {
            console.log("Expected error:", e)
        }
        console.log("Testing doAuthThing")
        MyKrosstalk.auth(BasicCredentials("username", "password")) {
            println(doAuthThing(3))
        }
        console.log("Testing doAuthThing, expecting error")
        try {
            println(doAuthThing(4))
        } catch (e: Exception) {
            console.log("Expected error:", e)
        }

        console.log("Testing doExt")
        println(4.doExt(5))
    }
}

actual object MyKrosstalk : Krosstalk(), KrosstalkClient<KtorClientScope<*>>, Scopes {
    override val serialization = KotlinxBinarySerializationHandler(Cbor { })
    override val client = KtorClient("http://localhost:8080")
    override val auth by scope(KtorClientBasicAuth())
}