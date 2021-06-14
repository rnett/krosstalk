package com.rnett.krosstalk.fullstack_test

import com.rnett.krosstalk.Headers
import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.Scope
import com.rnett.krosstalk.ScopeInstance
import com.rnett.krosstalk.ServerDefault
import com.rnett.krosstalk.WithHeaders
import com.rnett.krosstalk.annotations.EmptyBody
import com.rnett.krosstalk.annotations.ExceptionHandling
import com.rnett.krosstalk.annotations.ExplicitResult
import com.rnett.krosstalk.annotations.Ignore
import com.rnett.krosstalk.annotations.KrosstalkEndpoint
import com.rnett.krosstalk.annotations.KrosstalkMethod
import com.rnett.krosstalk.annotations.Optional
import com.rnett.krosstalk.annotations.PassObjects
import com.rnett.krosstalk.annotations.RequestHeaders
import com.rnett.krosstalk.annotations.RespondWithHeaders
import com.rnett.krosstalk.krosstalkPrefix
import com.rnett.krosstalk.methodName
import com.rnett.krosstalk.result.KrosstalkResult
import com.rnett.krosstalk.serialization.KotlinxBinarySerializationHandler
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

@Serializable
data class Data(val num: Int, val str: String)

data class ContextSerializable(val data: Int)

private object NonSerializableSerializer: KSerializer<ContextSerializable>{
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("com.rnett.krosstalk.fullstack_test.NonSerializable")

    override fun deserialize(decoder: Decoder): ContextSerializable {
        return ContextSerializable(decoder.decodeInt())
    }

    override fun serialize(encoder: Encoder, value: ContextSerializable) {
        encoder.encodeInt(value.data)
    }
}

abstract class BaseKrosstalk : Krosstalk() {
    override val serialization = KotlinxBinarySerializationHandler(Cbor {
        serializersModule += SerializersModule {
            contextual(NonSerializableSerializer)
        }
    })
}

expect object MyKrosstalk : BaseKrosstalk {
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
@ExplicitResult
@ExceptionHandling(propagateServerExceptions = true, includeStacktrace = true)
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
expect suspend fun withServerDefault(a: Int = 2, @Optional b: ServerDefault<Int> = ServerDefault { serverOnlyDefault() }): Int

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

@KrosstalkMethod(MyKrosstalk::class)
expect suspend fun withRequestHeaders(n: Int, @RequestHeaders h: Headers): Int

@KrosstalkMethod(MyKrosstalk::class)
@ExplicitResult
expect suspend fun withResultObject(n: Int): KrosstalkResult<ExpectObject>

@KrosstalkMethod(MyKrosstalk::class)
@ExplicitResult
expect suspend fun withSuccessOrHttpError(n: Int): KrosstalkResult.SuccessOrHttpError<Int>

@KrosstalkMethod(MyKrosstalk::class)
@ExplicitResult
expect suspend fun withSuccessOrServerException(n: Int): KrosstalkResult.SuccessOrServerException<Int>

@KrosstalkMethod(MyKrosstalk::class)
@ExplicitResult
expect suspend fun withHttpError(n: Int): KrosstalkResult.HttpError

@KrosstalkMethod(MyKrosstalk::class)
@ExplicitResult
@RespondWithHeaders
expect suspend fun withHttpErrorWithHeaders(n: Int): WithHeaders<KrosstalkResult.HttpError>

@KrosstalkMethod(MyKrosstalk::class)
@ExplicitResult
@RespondWithHeaders
expect suspend fun withSuccessOrServerExceptionWithHeaders(n: Int): KrosstalkResult.SuccessOrServerException<WithHeaders<Int>>

@KrosstalkMethod(MyKrosstalk::class)
expect suspend fun withNonKrosstalkHttpError(n: Int): Int

@KrosstalkMethod(MyKrosstalk::class)
expect suspend fun withNonKrosstalkServerException(n: Int): Int

@KrosstalkMethod(MyKrosstalk::class)
expect suspend fun withNonKrosstalkUncaughtException(n: Int): Int

@ExplicitResult
@KrosstalkMethod(MyKrosstalk::class)
expect suspend fun withUncaughtExceptionOutsideKrosstalkResult(n: Int): KrosstalkResult<Int>

@ExplicitResult
@KrosstalkMethod(MyKrosstalk::class)
expect suspend fun withHttpErrorOutsideKrosstalkResult(n: Int): KrosstalkResult<Int>

@KrosstalkMethod(MyKrosstalk::class)
expect suspend fun withRequestHeadersInCall(): String

@KrosstalkMethod(MyKrosstalk::class)
expect suspend fun withRequestHeadersInCallAndParam(@RequestHeaders headers: Headers, keys: Pair<String, String>): Pair<String?, String?>

@KrosstalkMethod(MyKrosstalk::class)
expect suspend fun withIgnored(@Ignore test: String = "test"): String

@KrosstalkMethod(MyKrosstalk::class)
expect suspend fun withIgnoredDependentDefault(pass: String, @Ignore ignore1: String = pass + "Ignore", @Ignore ignore2: String = ignore1 + "2"): Pair<String, String>

expect fun dependentServerDefault(pass: Int): String

@KrosstalkMethod(MyKrosstalk::class)
expect suspend fun withDependentServerDefault(pass: Int, @Optional test: ServerDefault<String> = ServerDefault { dependentServerDefault(pass) }): String

@KrosstalkMethod(MyKrosstalk::class)
expect suspend fun withContextSerializable(data: ContextSerializable): Int