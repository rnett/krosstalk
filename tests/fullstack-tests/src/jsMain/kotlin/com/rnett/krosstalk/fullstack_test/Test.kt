package com.rnett.krosstalk.fullstack_test

import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.KrosstalkResult
import com.rnett.krosstalk.Scope
import com.rnett.krosstalk.client.KrosstalkClient
import com.rnett.krosstalk.client.krosstalkCall
import com.rnett.krosstalk.fullstack_test.Data
import com.rnett.krosstalk.ktor.client.BasicCredentials
import com.rnett.krosstalk.ktor.client.KtorClient
import com.rnett.krosstalk.ktor.client.KtorClientAuth
import com.rnett.krosstalk.ktor.client.KtorClientScope
import com.rnett.krosstalk.serialization.KotlinxBinarySerializationHandler
import io.ktor.client.HttpClient
import io.ktor.client.features.auth.providers.basic
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logging
import io.ktor.http.authority
import kotlinx.serialization.cbor.Cbor
import kotlin.properties.Delegates

internal var lastUrl: String by Delegates.notNull()
    private set

internal var lastBody: Any by Delegates.notNull()
    private set

actual object MyKrosstalk : Krosstalk(), KrosstalkClient<KtorClientScope<*>> {
    actual override val serialization = KotlinxBinarySerializationHandler(Cbor { })
    override val client = object : KtorClient("http://localhost:8080") {
        override val baseClient: HttpClient = super.baseClient.config {
            Logging {
                level = LogLevel.BODY
            }
            this.defaultRequest {
                lastUrl = url.buildString().removePrefix(url.protocol.name).removePrefix("://").removePrefix(url.authority)
                lastBody = body
            }

        }
    }

    actual object Auth : Scope, KtorClientAuth<BasicCredentials>() {
        override fun io.ktor.client.features.auth.Auth.configureClientAuth(data: BasicCredentials) {
            basic {
                sendWithoutRequest = true
                username = data.username
                password = data.password
            }
        }
    }
}

actual suspend fun basicTest(data: Data): List<String> = krosstalkCall()

actual suspend fun basicEndpointTest(number: Int, str: String): String = krosstalkCall()

actual suspend fun endpointMethodTest(a: Int, b: Int): Int = krosstalkCall()

actual suspend fun emptyGet(): String = krosstalkCall()

actual suspend fun paramEndpointNoMinimize(a: Int, b: Int, c: Int, d: Int): Int = krosstalkCall()

actual suspend fun optionalEndpointNoMinimize(n: Int, s: String?): String? = krosstalkCall()

actual suspend fun optionalEndpointQueryParamsNoMinimize(n: Int, s: String?): String? = krosstalkCall()

actual suspend fun paramEndpointMinimize(a: Int, b: Int, c: Int, d: Int): Int = krosstalkCall()

actual suspend fun optionalEndpointMinimize(n: Int, s: String?): String? = krosstalkCall()

actual suspend fun optionalEndpointQueryParamsMinimize(n: Int, s: String?): String? = krosstalkCall()

actual suspend fun partialMinimize(n: Int, s: String?): String? = krosstalkCall()

actual suspend fun withResult(n: Int): KrosstalkResult<Int> = krosstalkCall()

actual suspend fun withResultCatching(n: Int): KrosstalkResult<Int> = krosstalkCall()