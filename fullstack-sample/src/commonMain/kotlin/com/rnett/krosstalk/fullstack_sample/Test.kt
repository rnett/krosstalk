package com.rnett.krosstalk.fullstack_sample

import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.KrosstalkResult
import com.rnett.krosstalk.ScopeHolder
import com.rnett.krosstalk.annotations.*
import com.rnett.krosstalk.serialization.KotlinxBinarySerializationHandler
import kotlinx.serialization.Serializable

@Serializable
data class Data(val num: Int, val str: String)

interface Scopes {
    val auth: ScopeHolder
}

expect object MyKrosstalk : Krosstalk, Scopes {
    override val serialization: KotlinxBinarySerializationHandler
}

@KrosstalkMethod(MyKrosstalk::class)
@KrosstalkEndpoint("/test/thing")
expect suspend fun doThing(data: Data): List<String>

@KrosstalkMethod(MyKrosstalk::class)
@KrosstalkEndpoint("/test/thing2", "GET")
@EmptyBody
expect suspend fun doEmptyThing(): Int

@KrosstalkMethod(MyKrosstalk::class)
@RequiredScopes("auth")
expect suspend fun doAuthThing(num: Int): Data

@KrosstalkMethod(MyKrosstalk::class)
expect suspend fun Int.doExt(other: Int): Double

@KrosstalkMethod(MyKrosstalk::class)
@ExplicitResult(true)
expect suspend fun doExplicitServerExceptionTest(): KrosstalkResult<Int>

@KrosstalkMethod(MyKrosstalk::class)
@RequiredScopes("auth")
@ExplicitResult
expect suspend fun doAuthHTTPExceptionTest(): KrosstalkResult<Int>

@KrosstalkMethod(MyKrosstalk::class)
@KrosstalkEndpoint("/endpointTest/{data}?value={value}", "GET")
@EmptyBody
expect suspend fun doEndpointTest(data: Data, value: Int): String