package com.rnett.krosstalk

import com.rnett.krosstalk.ktor.client.KtorClient
import com.rnett.krosstalk.ktor.client.KtorClientAuth
import com.rnett.krosstalk.ktor.client.KtorClientScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.reflect.typeOf

@OptIn(ExperimentalStdlibApi::class)
actual suspend fun doThing(data: Data): List<String> {
    return MyKrosstalk.call("doThing", mapOf("data" to data))
}

actual suspend fun doAuthThing(num: Int): Data {
    return MyKrosstalk.call("doAuthThing", mapOf("num" to num))
}

actual suspend fun Int.doExt(other: Int): Double {
    return MyKrosstalk.call("doExt", mapOf("other" to other), extensionReceiver = Optional.Some(this))
}


//TODO test instance receiver (should work)
fun main() {
    GlobalScope.launch {
        println(doThing(Data(3, "test")))
        try {
            println(doAuthThing(2))
        } catch (e: Exception) {
            console.log(e)
        }
        MyKrosstalk.auth(KtorClientAuth("username", "password")) {
            println(doAuthThing(3))
        }
        try {
            println(doAuthThing(4))
        } catch (e: Exception) {
            console.log(e)
        }

        println(4.doExt(5))
    }
}

@OptIn(ExperimentalStdlibApi::class)
actual object MyKrosstalk : Krosstalk(), KrosstalkClient<KtorClientScope>, Scopes {
    override val serialization = KotlinxSerializationHandler
    override val client = KtorClient
    override val auth by scope<KtorClientAuth>()

    //TODO registering methods should be handled by compiler plugin
    init {
        addMethod("doThing", ::doThing, MethodTypes(mapOf("data" to typeOf<Data>()), typeOf<List<String>>())) {}
        addMethod("doAuthThing", ::doAuthThing, MethodTypes(mapOf("num" to typeOf<Int>()), typeOf<Data>()), "auth") {}
        addMethod(
            "doExt",
            Int::doExt,
            MethodTypes(
                mapOf("other" to typeOf<Int>()),
                resultType = typeOf<Double>(),
                extensionReceiverType = typeOf<Int>()
            )
        ) {}
    }
}