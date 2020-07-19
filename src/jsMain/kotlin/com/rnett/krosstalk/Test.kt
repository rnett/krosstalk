package com.rnett.krosstalk

import com.rnett.krosstalk.ktor.KtorClientAuth
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.list
import kotlinx.serialization.builtins.serializer
import kotlin.browser.window
import kotlin.reflect.typeOf

@OptIn(ExperimentalStdlibApi::class)
actual suspend fun doThing(data: Data): List<String> {
    return MyKrosstalk.call("doThing", mapOf("data" to data))
}

actual suspend fun doAuthThing(num: Int): Data {
    return MyKrosstalk.call("doAuthThing", mapOf("num" to num))
}

fun main() {
    GlobalScope.launch {
        println(doThing(Data(3, "test")))
        try {
            println(doAuthThing(2))
        } catch (e: Exception){
            console.log(e)
        }
        MyKrosstalk.auth(KtorClientAuth("username", "password")){
            println(doAuthThing(3))
        }
        try {
            println(doAuthThing(4))
        } catch (e: Exception){
            console.log(e)
        }
    }
}