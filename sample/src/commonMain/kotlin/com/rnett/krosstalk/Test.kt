package com.rnett.krosstalk

import com.rnett.krosstalk.annotations.KrosstalkEndpoint
import com.rnett.krosstalk.annotations.KrosstalkMethod
import com.rnett.krosstalk.annotations.RequiredScopes
import kotlinx.serialization.Serializable

@Serializable
data class Data(val num: Int, val str: String)

interface Scopes {
    val auth: ScopeHolder
}

expect object MyKrosstalk : Krosstalk, Scopes

@KrosstalkMethod(MyKrosstalk::class)
@KrosstalkEndpoint("/test/thing")
expect suspend fun doThing(data: Data): List<String>

@KrosstalkMethod(MyKrosstalk::class)
@RequiredScopes("auth")
expect suspend fun doAuthThing(num: Int): Data

@KrosstalkMethod(MyKrosstalk::class)
expect suspend fun Int.doExt(other: Int): Double