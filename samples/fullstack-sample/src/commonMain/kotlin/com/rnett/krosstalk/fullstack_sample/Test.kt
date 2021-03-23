package com.rnett.krosstalk.fullstack_sample

import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.KrosstalkResult
import com.rnett.krosstalk.Scope
import com.rnett.krosstalk.ScopeInstance
import com.rnett.krosstalk.annotations.EmptyBody
import com.rnett.krosstalk.annotations.ExplicitResult
import com.rnett.krosstalk.annotations.KrosstalkEndpoint
import com.rnett.krosstalk.annotations.KrosstalkMethod
import com.rnett.krosstalk.serialization.KotlinxBinarySerializationHandler
import kotlinx.serialization.Serializable

@Serializable
data class Data(val num: Int, val str: String)

expect object MyKrosstalk : Krosstalk {
    override val serialization: KotlinxBinarySerializationHandler

    object Auth : Scope
}

@KrosstalkMethod(MyKrosstalk::class)
//@KrosstalkEndpoint("/test/thing")
expect suspend fun doThing(data: Data): List<String>

@KrosstalkMethod(MyKrosstalk::class)
@KrosstalkEndpoint("/test/thing2", "GET")
@EmptyBody
expect suspend fun doEmptyThing(): Int

@KrosstalkMethod(MyKrosstalk::class)
expect suspend fun doAuthThing(num: Int, auth: ScopeInstance<MyKrosstalk.Auth>): Data

@KrosstalkMethod(MyKrosstalk::class)
expect suspend fun Int.doExt(other: Int): Double

@KrosstalkMethod(MyKrosstalk::class)
@ExplicitResult(true)
expect suspend fun doExplicitServerExceptionTest(): KrosstalkResult<Int>

@KrosstalkMethod(MyKrosstalk::class)
@ExplicitResult
expect suspend fun doAuthHTTPExceptionTest(auth: ScopeInstance<MyKrosstalk.Auth>): KrosstalkResult<Int>

@KrosstalkMethod(MyKrosstalk::class)
@KrosstalkEndpoint("/endpointTest/{data}?value={value}", "GET")
@EmptyBody
expect suspend fun doEndpointTest(data: Data, value: Int): String