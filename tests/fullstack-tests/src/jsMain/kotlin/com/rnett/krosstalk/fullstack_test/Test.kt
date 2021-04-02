package com.rnett.krosstalk.fullstack_test

import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.KrosstalkOptional
import com.rnett.krosstalk.KrosstalkResult
import com.rnett.krosstalk.Scope
import com.rnett.krosstalk.ScopeInstance
import com.rnett.krosstalk.client.KrosstalkClient
import com.rnett.krosstalk.client.krosstalkCall
import com.rnett.krosstalk.ktor.client.KtorClient
import com.rnett.krosstalk.ktor.client.KtorClientBasicAuth
import com.rnett.krosstalk.ktor.client.KtorClientScope
import com.rnett.krosstalk.serialization.KotlinxBinarySerializationHandler
import io.ktor.client.HttpClient
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.authority
import io.ktor.http.contentType
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

actual object MyKrosstalk : Krosstalk(), KrosstalkClient<KtorClientScope<*>> {
    actual override val serialization = KotlinxBinarySerializationHandler(Cbor { })
    override val client = KtorClient(
        "http://localhost:8080",
        baseClient = HttpClient().config {
            Logging {
                level = LogLevel.BODY
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

actual suspend fun withKrosstalkOptional(a: Int, b: KrosstalkOptional<Int>): Int = krosstalkCall()

actual suspend fun withKrosstalkOptionalDefault(a: Int, b: KrosstalkOptional<Int>): Int = krosstalkCall()

actual fun serverOnlyDefault(): KrosstalkOptional<Int> = error("Called on client side")
actual suspend fun withKrosstalkOptionalServerDefault(a: Int, b: KrosstalkOptional<Int>): Int = krosstalkCall()