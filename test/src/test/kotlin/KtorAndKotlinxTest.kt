package com.rnett.krosstalk.test

import com.rnett.krosstalk.client.RequestMaker
import com.rnett.krosstalk.serialization.KrosstalkClientSerialization
import com.rnett.krosstalk.serialization.KrosstalkServerSerialization
import com.rnett.krosstalk.server.mount
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class KtorAndKotlinxTest {

    @Test
    fun `basic request works`() = testApplication {

        val server = object : KtorAndKotlinxServer(KrosstalkServerSerialization(Json)) {
            override suspend fun testEndpoint(request: TestRequest): TestResponse {
                return TestResponse(request.i + request.data.size, request.data.joinToString(","))
            }
        }

        routing {
            route("my/test") {
                mount(server)
            }
        }

        val client = KtorAndKotlinxClient("my/test", RequestMaker(client), KrosstalkClientSerialization(Json))

        assertEquals(TestResponse(5, "a,b,c"), client.testEndpoint(TestRequest(listOf("a", "b", "c"), 2)))
    }

}