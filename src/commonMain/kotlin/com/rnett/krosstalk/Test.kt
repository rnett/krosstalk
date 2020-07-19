package com.rnett.krosstalk

import com.rnett.krosstalk.ktor.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.list
import kotlinx.serialization.builtins.serializer

@Serializable
data class Data(val num: Int, val str: String)

object MyKrosstalk : Krosstalk<KotlinxSerializers, KtorClientScope, KtorServerScope>() {
    override val serialization = KotlinxSerializationHandler
    override val client = KtorClient
    override val server = KtorServer

    val auth = clientScope<KtorClientAuth>() + KtorServerAuth(mapOf("username" to "password"))

    init {
        addMethod("doThing", ::doThing, KotlinxSerializers(mapOf("data" to Data.serializer()), String.serializer().list))
        addMethod("doAuthThing", ::doAuthThing, KotlinxSerializers(mapOf("num" to Int.serializer()), Data.serializer()), auth)
    }
}

expect suspend fun doThing(data: Data): List<String>
expect suspend fun doAuthThing(num: Int): Data