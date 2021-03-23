package com.rnett.krosstalk.fullstack_sample

import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.KrosstalkClient
import com.rnett.krosstalk.KrosstalkResult
import com.rnett.krosstalk.Scope
import com.rnett.krosstalk.ScopeInstance
import com.rnett.krosstalk.invoke
import com.rnett.krosstalk.krosstalkCall
import com.rnett.krosstalk.ktor.client.BasicCredentials
import com.rnett.krosstalk.ktor.client.KtorClient
import com.rnett.krosstalk.ktor.client.KtorClientAuth
import com.rnett.krosstalk.ktor.client.KtorClientScope
import com.rnett.krosstalk.serialization.KotlinxBinarySerializationHandler
import io.ktor.client.HttpClient
import io.ktor.client.features.auth.providers.basic
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logging
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.cbor.Cbor

actual suspend fun doThing(data: Data): List<String> = krosstalkCall()

actual suspend fun doEmptyThing(): Int = krosstalkCall()

actual suspend fun doAuthThing(num: Int, auth: ScopeInstance<MyKrosstalk.Auth>): Data = krosstalkCall()

actual suspend fun Int.doExt(other: Int): Double = krosstalkCall()

actual suspend fun doExplicitServerExceptionTest(): KrosstalkResult<Int> = krosstalkCall()

actual suspend fun doAuthHTTPExceptionTest(auth: ScopeInstance<MyKrosstalk.Auth>): KrosstalkResult<Int> = krosstalkCall()

actual suspend fun doEndpointTest(data: Data, value: Int): String = krosstalkCall()

//TODO test instance receiver (should work)
fun main() {
    GlobalScope.launch {
        console.log("Testing doThing")
        println(doThing(Data(3, "test")))

        console.log("Testing doEmptyThing")
        println(doEmptyThing())

        console.log("Testing doAuthThing, expecting error")
        try {
            println(doAuthThing(2, MyKrosstalk.Auth(BasicCredentials("u", "p"))))
        } catch (e: Exception) {
            console.log("Expected error:", e)
        }
        console.log("Testing doAuthThing")
//        MyKrosstalk.auth(BasicCredentials("username", "password")) {
//            println(doAuthThing(3))
//        }
        console.log("Testing doAuthThing, expecting error")
//        try {
//            println(doAuthThing(4))
//        } catch (e: Exception) {
//            console.log("Expected error:", e)
//            println()
//        }

        console.log("Testing doExt")
        println(4.doExt(5))

        console.log("Testing doExplicitServerExceptionTest")
        val x = doExplicitServerExceptionTest()
        console.log(x)

        console.log("Testing doExplicitServerExceptionTest")
//        MyKrosstalk.auth(BasicCredentials("test", "fail")) {
//            val x = doAuthHTTPExceptionTest()
//            console.log(x)
//        }

        console.log("Testing doEndpontTest")
        println(doEndpointTest(Data(10, "test"), 3))

    }
}

actual object MyKrosstalk : Krosstalk(), KrosstalkClient<KtorClientScope<*>> {
    actual override val serialization = KotlinxBinarySerializationHandler(Cbor { })
    override val client = object : KtorClient("http://localhost:8080") {
        override val baseClient: HttpClient = super.baseClient.config {
            Logging {
                level = LogLevel.INFO
            }
        }
    }

    actual object Auth : Scope, KtorClientAuth<BasicCredentials>() {
        override fun io.ktor.client.features.auth.Auth.configureClientAuth(data: BasicCredentials) {
            basic {

                sendWithoutRequest = true
                username = data.username
                password = data.password
            }
        }
    }
}