package com.rnett.krosstalk.fullstack_test

import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.KrosstalkResult
import com.rnett.krosstalk.Scope
import com.rnett.krosstalk.ScopeInstance
import com.rnett.krosstalk.annotations.EmptyBody
import com.rnett.krosstalk.annotations.ExplicitResult
import com.rnett.krosstalk.annotations.KrosstalkEndpoint
import com.rnett.krosstalk.annotations.KrosstalkMethod
import com.rnett.krosstalk.annotations.MinimizeBody
import com.rnett.krosstalk.krosstalkPrefix
import com.rnett.krosstalk.methodName
import com.rnett.krosstalk.serialization.KotlinxBinarySerializationHandler
import kotlinx.serialization.Serializable

//TODO want some way to support binary serialization for body, but string for args (URL Encoded JSON?).  Have separate body and url serializers?

//TODO I'd like to be able to test the used URLs
@Serializable
data class Data(val num: Int, val str: String)

expect object MyKrosstalk : Krosstalk {
    override val serialization: KotlinxBinarySerializationHandler

    object Auth : Scope
}

@KrosstalkMethod(MyKrosstalk::class)
expect suspend fun basicTest(data: Data): List<String>

@KrosstalkMethod(MyKrosstalk::class)
@KrosstalkEndpoint("/base/$methodName/test")
expect suspend fun basicEndpointTest(number: Int, str: String): String

@KrosstalkMethod(MyKrosstalk::class)
@KrosstalkEndpoint(httpMethod = "PUT")
expect suspend fun endpointMethodTest(a: Int, b: Int): Int

@KrosstalkMethod(MyKrosstalk::class)
@KrosstalkEndpoint(contentType = "text/plain")
expect suspend fun endpointContentTypeTest(a: Int, b: Int): Int

@KrosstalkMethod(MyKrosstalk::class)
@KrosstalkEndpoint(httpMethod = "GET")
expect suspend fun emptyGet(): String

@KrosstalkMethod(MyKrosstalk::class)
@KrosstalkEndpoint("$krosstalkPrefix/$methodName/a/{a}/{{b}}?c={c}&{{d}}")
expect suspend fun paramEndpointNoMinimize(a: Int, b: Int, c: Int, d: Int): Int

@KrosstalkMethod(MyKrosstalk::class)
@KrosstalkEndpoint("$krosstalkPrefix/$methodName/{{n}}/{{?s}}")
expect suspend fun optionalEndpointNoMinimize(n: Int, s: String?): String?

@KrosstalkMethod(MyKrosstalk::class)
@KrosstalkEndpoint("$krosstalkPrefix/$methodName/?{{n}}&{{?s}}")
expect suspend fun optionalEndpointQueryParamsNoMinimize(n: Int, s: String?): String?

@KrosstalkMethod(MyKrosstalk::class)
@KrosstalkEndpoint("$krosstalkPrefix/$methodName/a/{a}/{{b}}?c={c}&{{d}}", httpMethod = "GET")
@EmptyBody
expect suspend fun paramEndpointMinimize(a: Int, b: Int, c: Int, d: Int): Int

@KrosstalkMethod(MyKrosstalk::class)
@KrosstalkEndpoint("$krosstalkPrefix/$methodName/{{n}}/{{?s}}", httpMethod = "GET")
@EmptyBody
expect suspend fun optionalEndpointMinimize(n: Int, s: String?): String?

@KrosstalkMethod(MyKrosstalk::class)
@KrosstalkEndpoint("$krosstalkPrefix/$methodName/?{{n}}&{{?s}}", httpMethod = "GET")
@EmptyBody
expect suspend fun optionalEndpointQueryParamsMinimize(n: Int, s: String?): String?

@KrosstalkMethod(MyKrosstalk::class)
@KrosstalkEndpoint("$krosstalkPrefix/$methodName/?{{n}}")
@MinimizeBody
expect suspend fun partialMinimize(n: Int, s: String?): String?

//TODO failing b/c js sealed serialization issue?
@KrosstalkMethod(MyKrosstalk::class)
@ExplicitResult(propagateServerExceptions = true)
expect suspend fun withResult(n: Int): KrosstalkResult<Int>

class MyException(message: String) : RuntimeException(message)

//TODO failing b/c js sealed serialization issue?
@KrosstalkMethod(MyKrosstalk::class)
@ExplicitResult
expect suspend fun withResultCatching(n: Int): KrosstalkResult<Int>

@KrosstalkMethod(MyKrosstalk::class)
expect suspend fun withOverload(n: Int): String

@KrosstalkMethod(MyKrosstalk::class)
expect suspend fun withOverload(s: String): Int

@KrosstalkMethod(MyKrosstalk::class)
expect suspend fun withAuth(n: Int, auth: ScopeInstance<MyKrosstalk.Auth>): String

@KrosstalkMethod(MyKrosstalk::class)
expect suspend fun withOptionalAuth(auth: ScopeInstance<MyKrosstalk.Auth>?): String?