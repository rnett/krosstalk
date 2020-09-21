package com.rnett.krosstalk

import com.rnett.krosstalk.annotations.KrosstalkMethod
import kotlinx.serialization.Serializable

@Serializable
data class Data(val num: Int, val str: String)

interface Scopes {
    val auth: ScopeHolder
}

expect object MyKrosstalk : Krosstalk, Scopes

@KrosstalkMethod(MyKrosstalk::class)
expect suspend fun doThing(data: Data): List<String>

@KrosstalkMethod(MyKrosstalk::class, "auth")
expect suspend fun doAuthThing(num: Int): Data

@KrosstalkMethod(MyKrosstalk::class)
expect suspend fun Int.doExt(other: Int): Double