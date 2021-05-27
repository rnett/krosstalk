package com.rnett.krosstalk.fullstack_test

import com.rnett.krosstalk.Headers
import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.Scope
import com.rnett.krosstalk.ScopeInstance
import com.rnett.krosstalk.ServerDefault
import com.rnett.krosstalk.WithHeaders
import com.rnett.krosstalk.client.KrosstalkClient
import com.rnett.krosstalk.client.krosstalkCall
import com.rnett.krosstalk.headersOf
import com.rnett.krosstalk.ktor.client.KtorClient
import com.rnett.krosstalk.ktor.client.KtorClientScope
import com.rnett.krosstalk.ktor.client.auth.KtorClientBasicAuth
import com.rnett.krosstalk.result.KrosstalkResult
import com.rnett.krosstalk.serialization.KotlinxBinarySerializationHandler
import com.rnett.krosstalk.toHeaders
import io.ktor.client.HttpClient
import io.ktor.client.features.HttpResponseValidator
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.authority
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlin.properties.Delegates

internal var lastUrl: String by Delegates.notNull()
    private set

internal var lastBody: Any by Delegates.notNull()
    private set

internal var lastHttpMethod: HttpMethod by Delegates.notNull()
    private set

internal var lastContentType: ContentType? = null
    private set

internal var lastStatusCode: Int? = null
    private set

internal var lastHeaders: Headers? = null
    private set

actual object MyKrosstalk : Krosstalk(), KrosstalkClient<KtorClientScope<*>> {
    actual override val serialization = KotlinxBinarySerializationHandler(Cbor { })
    override val serverUrl: String = "http://localhost:8080"

    override val client = KtorClient(
        baseClient = HttpClient().config {
            Logging {
                level = LogLevel.ALL
            }
            HttpResponseValidator {
                validateResponse {
                    lastStatusCode = it.status.value
                }
            }
            defaultRequest {
                lastHeaders = this.headers.entries().associate { it.key to it.value }.toHeaders()
            }
        },
        baseRequest = {
            lastUrl = url.buildString().removePrefix(url.protocol.name).removePrefix("://").removePrefix(url.authority)
            lastBody = body
            lastHttpMethod = this.method
            lastContentType = this.contentType()
        })

    actual object Auth : Scope, KtorClientBasicAuth()
}

actual suspend fun basicTest(data: Data): List<String> = krosstalkCall()

actual suspend fun basicEndpointTest(number: Int, str: String): String = krosstalkCall()

actual suspend fun endpointMethodTest(a: Int, b: Int): Int = krosstalkCall()

actual suspend fun endpointContentTypeTest(a: Int, b: Int): Int = krosstalkCall()

actual suspend fun emptyGet(): String = krosstalkCall()

actual suspend fun paramEndpoint(a: Int, b: Int, c: Int, d: Int): Int = krosstalkCall()

actual suspend fun optionalEndpoint(n: Int, s: String?): String? = krosstalkCall()

actual suspend fun optionalEndpointQueryParams(n: Int, s: String?): String? = krosstalkCall()

actual suspend fun paramEndpointGet(a: Int, b: Int, c: Int, d: Int): Int = krosstalkCall()

actual suspend fun optionalEndpointGet(n: Int, s: String?): String? = krosstalkCall()

actual suspend fun optionalEndpointQueryParamsGet(n: Int, s: String?): String? = krosstalkCall()

actual suspend fun partialMinimize(n: Int, s: String?): String? = krosstalkCall()

actual suspend fun withResult(n: Int): KrosstalkResult<Int> = krosstalkCall()

actual suspend fun withResultCatching(n: Int): KrosstalkResult<Int> = krosstalkCall()

actual suspend fun withOverload(n: Int): String = krosstalkCall()

actual suspend fun withOverload(s: String): Int = krosstalkCall()

actual suspend fun withAuth(
    n: Int,
    auth: ScopeInstance<MyKrosstalk.Auth>,
): String = krosstalkCall()

actual suspend fun withOptionalAuth(auth: ScopeInstance<MyKrosstalk.Auth>?): String? = krosstalkCall()

actual suspend fun Int?.withOptionalReceiver(s: String?): String = krosstalkCall()

actual suspend fun withOptionalDefault(a: Int, b: Int?): Int = krosstalkCall()

actual fun serverOnlyDefault(): Int = error("Called on client side")
actual suspend fun withServerDefault(a: Int, b: ServerDefault<Int>): Int = krosstalkCall()

actual object ExpectObject {
    actual fun value(): Int = 4

    actual suspend fun withExpectObjectParam(): Int = krosstalkCall()
}

actual suspend fun withExpectObjectValueParam(p: ExpectObject): Int = krosstalkCall()

@Serializable
actual object SerializableObject {
    actual val value: Int = 10
}

actual suspend fun withPassedExpectObjectValueParam(p: SerializableObject): Int = krosstalkCall()

actual suspend fun withUnitReturn(s: String): Unit = krosstalkCall()

actual suspend fun withObjectReturn(s: String): ExpectObject = krosstalkCall()

actual suspend fun withPassedObjectReturn(s: String): SerializableObject = krosstalkCall()

actual suspend fun withDifferentPassing(arg: SerializableObject): ExpectObject = krosstalkCall()

actual suspend fun withHeadersBasic(n: Int): WithHeaders<String> = krosstalkCall()

actual suspend fun withHeadersOutsideResult(n: Int): WithHeaders<KrosstalkResult<String>> = krosstalkCall()

actual suspend fun withHeadersInsideResult(n: Int): KrosstalkResult<WithHeaders<String>> = krosstalkCall()

actual suspend fun withHeadersReturnObject(n: Int): WithHeaders<ExpectObject> = krosstalkCall()

actual suspend fun withRequestHeaders(n: Int, h: Headers): Int = krosstalkCall()

actual suspend fun withResultObject(n: Int): KrosstalkResult<ExpectObject> = krosstalkCall()

actual suspend fun withSuccessOrHttpError(n: Int): KrosstalkResult.SuccessOrHttpError<Int> = krosstalkCall()

actual suspend fun withSuccessOrServerException(n: Int): KrosstalkResult.SuccessOrServerException<Int> = krosstalkCall()

actual suspend fun withHttpError(n: Int): KrosstalkResult.HttpError = krosstalkCall()

actual suspend fun withHttpErrorWithHeaders(n: Int): WithHeaders<KrosstalkResult.HttpError> = krosstalkCall()

actual suspend fun withSuccessOrServerExceptionWithHeaders(n: Int): KrosstalkResult.SuccessOrServerException<WithHeaders<Int>> = krosstalkCall()

actual suspend fun withNonKrosstalkHttpError(n: Int): Int = krosstalkCall()

actual suspend fun withNonKrosstalkServerException(n: Int): Int = krosstalkCall()

actual suspend fun withNonKrosstalkUncaughtException(n: Int): Int = krosstalkCall()

actual suspend fun withUncaughtExceptionOutsideKrosstalkResult(n: Int): KrosstalkResult<Int> = krosstalkCall()

actual suspend fun withHttpErrorOutsideKrosstalkResult(n: Int): KrosstalkResult<Int> = krosstalkCall()

actual suspend fun withRequestHeadersInCall(): String = krosstalkCall(headersOf("test" to "2"))

actual suspend fun withRequestHeadersInCallAndParam(
    headers: Headers,
    keys: Pair<String, String>
): Pair<String?, String?> = krosstalkCall(headersOf(keys.first to "call"))

actual suspend fun withIgnored(test: String): String = krosstalkCall()

actual suspend fun withIgnoredDependentDefault(
    pass: String,
    ignore1: String,
    ignore2: String
): Pair<String, String> = krosstalkCall()

actual fun dependentServerDefault(pass: Int): String = error("Called server default on the client side")

actual suspend fun withDependentServerDefault(pass: Int, test: ServerDefault<String>): String = krosstalkCall()