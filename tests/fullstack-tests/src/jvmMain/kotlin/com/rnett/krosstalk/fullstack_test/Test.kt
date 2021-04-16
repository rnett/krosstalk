package com.rnett.krosstalk.fullstack_test

import com.rnett.krosstalk.Headers
import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.KrosstalkResult
import com.rnett.krosstalk.Scope
import com.rnett.krosstalk.ScopeInstance
import com.rnett.krosstalk.ServerDefault
import com.rnett.krosstalk.WithHeaders
import com.rnett.krosstalk.ktor.server.KtorServer
import com.rnett.krosstalk.ktor.server.KtorServerBasicAuth
import com.rnett.krosstalk.ktor.server.KtorServerScope
import com.rnett.krosstalk.ktor.server.defineKtor
import com.rnett.krosstalk.runKrosstalkCatching
import com.rnett.krosstalk.serialization.KotlinxBinarySerializationHandler
import com.rnett.krosstalk.server.KrosstalkServer
import com.rnett.krosstalk.server.value
import com.rnett.krosstalk.toKrosstalkResult
import io.ktor.application.install
import io.ktor.auth.Principal
import io.ktor.features.CORS
import io.ktor.features.CallLogging
import io.ktor.routing.routing
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import org.slf4j.event.Level


fun main() {
    embeddedServer(CIO, 8080, "localhost") {

        install(CORS) {
            anyHost()
        }
        install(CallLogging) {
            level = Level.DEBUG
        }

        routing {
//            trace { application.log.info(it.buildText()) }
            MyKrosstalk.defineKtor(this)
        }

    }.start(true)
}

data class User(val username: String) : Principal

private val validUsers = mapOf("username" to "password")

actual object MyKrosstalk : Krosstalk(), KrosstalkServer<KtorServerScope<*>> {
    actual override val serialization = KotlinxBinarySerializationHandler(Cbor { })
    override val server = KtorServer

    actual object Auth : Scope, KtorServerBasicAuth<User>("auth", {
        validate {
            if (validUsers[it.name] == it.password)
                User(it.name)
            else
                null
        }
    })
}

actual suspend fun basicTest(data: Data): List<String> {
    return List(data.num) { data.str }
}

actual suspend fun basicEndpointTest(number: Int, str: String): String = str.repeat(number)

actual suspend fun endpointMethodTest(a: Int, b: Int): Int = a + b

actual suspend fun endpointContentTypeTest(a: Int, b: Int): Int = a + b

actual suspend fun emptyGet(): String = "Hello World!"

actual suspend fun paramEndpoint(a: Int, b: Int, c: Int, d: Int): Int = a * b * c * d

actual suspend fun optionalEndpoint(n: Int, s: String?): String? = s?.repeat(n)

actual suspend fun optionalEndpointQueryParams(n: Int, s: String?): String? = s?.repeat(n)

actual suspend fun paramEndpointGet(a: Int, b: Int, c: Int, d: Int): Int = a * b * c * d

actual suspend fun optionalEndpointGet(n: Int, s: String?): String? = s?.repeat(n)

actual suspend fun optionalEndpointQueryParamsGet(n: Int, s: String?): String? = s?.repeat(n)

actual suspend fun partialMinimize(n: Int, s: String?): String? = s?.repeat(n)

actual suspend fun withResult(n: Int): KrosstalkResult<Int> = runCatching {
    if (n < 0)
        error("Can't have n < 0")

    return@runCatching n
}.toKrosstalkResult()

actual suspend fun withResultCatching(n: Int): KrosstalkResult<Int> = runKrosstalkCatching {
    if (n < 0)
        throw MyException("Can't have n < 0")

    n
}.catchAsHttpStatusCode<MyException> { 422 }

actual suspend fun withOverload(n: Int): String = n.toString()

actual suspend fun withOverload(s: String): Int = s.toInt()

actual suspend fun withAuth(
    n: Int,
    auth: ScopeInstance<MyKrosstalk.Auth>,
): String {
    return auth.value.username
}

actual suspend fun withOptionalAuth(auth: ScopeInstance<MyKrosstalk.Auth>?): String? {
    return auth?.value?.username
}

actual suspend fun Int?.withOptionalReceiver(s: String?): String {
    return (s ?: "a").repeat(this ?: 2)
}

actual suspend fun withOptionalDefault(a: Int, b: Int?): Int = a * (b ?: 0)

actual fun serverOnlyDefault(): Int = 4
actual suspend fun withServerDefault(a: Int, b: ServerDefault<Int>): Int = a * b.value

actual object ExpectObject {
    actual fun value(): Int = 10

    actual suspend fun withExpectObjectParam(): Int = value()

}

actual suspend fun withExpectObjectValueParam(p: ExpectObject): Int = p.value()

@Serializable
actual object SerializableObject {
    actual val value: Int = 3
}

actual suspend fun withPassedExpectObjectValueParam(p: SerializableObject): Int = p.value

actual suspend fun withUnitReturn(s: String) {
}

actual suspend fun withObjectReturn(s: String): ExpectObject = ExpectObject

actual suspend fun withPassedObjectReturn(s: String): SerializableObject = SerializableObject

actual suspend fun withDifferentPassing(arg: SerializableObject): ExpectObject = ExpectObject

actual suspend fun withHeadersBasic(n: Int): WithHeaders<String> = WithHeaders(n.toString(), mapOf("test" to listOf("value")))

actual suspend fun withHeadersOutsideResult(n: Int): WithHeaders<KrosstalkResult<String>> = WithHeaders(runKrosstalkCatching {
    if (n < 0)
        throw MyException("Can't have n < 0")

    n.toString()
}.catchAsHttpStatusCode<MyException> { 422 },
    mapOf("test" to listOf("value")))

actual suspend fun withHeadersInsideResult(n: Int): KrosstalkResult<WithHeaders<String>> = runKrosstalkCatching {
    if (n < 0)
        throw MyException("Can't have n < 0")

    WithHeaders(n.toString(), mapOf("test" to listOf("value")))
}.catchAsHttpStatusCode<MyException> { 422 }

actual suspend fun withHeadersReturnObject(n: Int): WithHeaders<ExpectObject> {
    return WithHeaders(ExpectObject, mapOf("value" to listOf(n.toString())))
}

actual suspend fun withRequestHeaders(n: Int, h: Headers): Int {
    return n * h["value"]!!.first().toInt()
}

actual suspend fun withResultObject(n: Int): KrosstalkResult<ExpectObject> = runKrosstalkCatching {
    if (n < 0)
        throw MyException("Can't have n < 0")

    ExpectObject
}