package com.rnett.krosstalk.fullstack_test

import com.rnett.krosstalk.Headers
import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.KrosstalkResult
import com.rnett.krosstalk.Scope
import com.rnett.krosstalk.ScopeInstance
import com.rnett.krosstalk.ServerDefault
import com.rnett.krosstalk.WithHeaders
import com.rnett.krosstalk.annotations.EmptyBody
import com.rnett.krosstalk.annotations.ExplicitResult
import com.rnett.krosstalk.annotations.KrosstalkEndpoint
import com.rnett.krosstalk.annotations.KrosstalkMethod
import com.rnett.krosstalk.annotations.Optional
import com.rnett.krosstalk.annotations.PassObjects
import com.rnett.krosstalk.annotations.RequestHeaders
import com.rnett.krosstalk.annotations.RespondWithHeaders
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
expect suspend fun paramEndpoint(a: Int, b: Int, c: Int, d: Int): Int

@KrosstalkMethod(MyKrosstalk::class)
@KrosstalkEndpoint("$krosstalkPrefix/$methodName/{{n}}/{{?s}}")
expect suspend fun optionalEndpoint(n: Int, @Optional s: String?): String?

@KrosstalkMethod(MyKrosstalk::class)
@KrosstalkEndpoint("$krosstalkPrefix/$methodName/?{{n}}&{{?s}}")
expect suspend fun optionalEndpointQueryParams(n: Int, @Optional s: String?): String?

@KrosstalkMethod(MyKrosstalk::class)
@KrosstalkEndpoint("$krosstalkPrefix/$methodName/a/{a}/{{b}}?c={c}&{{d}}", httpMethod = "GET")
@EmptyBody
expect suspend fun paramEndpointGet(a: Int, b: Int, c: Int, d: Int): Int

@KrosstalkMethod(MyKrosstalk::class)
@KrosstalkEndpoint("$krosstalkPrefix/$methodName/{{n}}/{{?s}}", httpMethod = "GET")
@EmptyBody
expect suspend fun optionalEndpointGet(n: Int, @Optional s: String?): String?

@KrosstalkMethod(MyKrosstalk::class)
@KrosstalkEndpoint("$krosstalkPrefix/$methodName/?{{n}}&{{?s}}", httpMethod = "GET")
@EmptyBody
expect suspend fun optionalEndpointQueryParamsGet(n: Int, @Optional s: String?): String?

@KrosstalkMethod(MyKrosstalk::class)
@KrosstalkEndpoint("$krosstalkPrefix/$methodName/?{{n}}")
expect suspend fun partialMinimize(n: Int, @Optional s: String?): String?

@KrosstalkMethod(MyKrosstalk::class)
@ExplicitResult(propagateServerExceptions = true)
expect suspend fun withResult(n: Int): KrosstalkResult<Int>

class MyException(message: String) : RuntimeException(message)

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

@KrosstalkMethod(MyKrosstalk::class)
expect suspend fun @receiver:Optional Int?.withOptionalReceiver(s: String?): String

@KrosstalkMethod(MyKrosstalk::class)
expect suspend fun withOptionalDefault(a: Int = 2, @Optional b: Int? = 4): Int

expect fun serverOnlyDefault(): Int

@KrosstalkEndpoint("$krosstalkPrefix/$methodName/{{a}}/{{?b}}")
@KrosstalkMethod(MyKrosstalk::class)
@EmptyBody
expect suspend fun withServerDefault(a: Int = 2, b: ServerDefault<Int> = ServerDefault { serverOnlyDefault() }): Int

expect object ExpectObject {
    fun value(): Int

    @KrosstalkMethod(MyKrosstalk::class)
    suspend fun withExpectObjectParam(): Int
}

@KrosstalkMethod(MyKrosstalk::class)
expect suspend fun withExpectObjectValueParam(p: ExpectObject): Int

@Serializable
expect object SerializableObject {
    val value: Int
}

@KrosstalkMethod(MyKrosstalk::class)
@PassObjects
expect suspend fun withPassedExpectObjectValueParam(p: SerializableObject): Int

@KrosstalkMethod(MyKrosstalk::class)
expect suspend fun withUnitReturn(s: String): Unit


@KrosstalkMethod(MyKrosstalk::class)
expect suspend fun withObjectReturn(s: String): ExpectObject


@KrosstalkMethod(MyKrosstalk::class)
@PassObjects(returnToo = true)
expect suspend fun withPassedObjectReturn(s: String): SerializableObject

@KrosstalkMethod(MyKrosstalk::class)
@PassObjects
expect suspend fun withDifferentPassing(arg: SerializableObject): ExpectObject

@RespondWithHeaders
@KrosstalkMethod(MyKrosstalk::class)
expect suspend fun withHeadersBasic(n: Int): WithHeaders<String>

@RespondWithHeaders
@ExplicitResult
@KrosstalkMethod(MyKrosstalk::class)
expect suspend fun withHeadersOutsideResult(n: Int): WithHeaders<KrosstalkResult<String>>

@RespondWithHeaders
@ExplicitResult
@KrosstalkMethod(MyKrosstalk::class)
expect suspend fun withHeadersInsideResult(n: Int): KrosstalkResult<WithHeaders<String>>

@RespondWithHeaders
@KrosstalkMethod(MyKrosstalk::class)
expect suspend fun withHeadersReturnObject(n: Int): WithHeaders<ExpectObject>

//TODO KrosstalkResult + object

@KrosstalkMethod(MyKrosstalk::class)
expect suspend fun withRequestHeaders(n: Int, @RequestHeaders h: Headers): Int
