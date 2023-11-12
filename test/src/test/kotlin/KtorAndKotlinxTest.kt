package com.rnett.krosstalk.test

import com.rnett.krosstalk.client.RequestMaker
import com.rnett.krosstalk.serialization.KrosstalkClientSerialization
import com.rnett.krosstalk.serialization.KrosstalkServerSerialization
import com.rnett.krosstalk.server.mount
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class KtorAndKotlinxTest {

    private val server = object : KtorAndKotlinxServer(KrosstalkServerSerialization(Json)) {
        override suspend fun testEndpoint(request: TestRequest): TestResponse {
            return TestResponse(request.i + request.data.size, request.data.joinToString(","))
        }
    }

    @Test
    fun `basic request works`() = testApplication {

        routing {
            route("my/test") {
                mount(server)
            }
        }

        val client = KtorAndKotlinxClient("my/test", RequestMaker(client), KrosstalkClientSerialization(Json))

        assertEquals(TestResponse(5, "a,b,c"), client.testEndpoint(TestRequest(listOf("a", "b", "c"), 2)))
    }

    @Test
    fun `works with auth`() = testApplication {
        install(Authentication) {
            basic {
                validate {
                    if (it.name == "test" && it.password == "password")
                        return@validate object : Principal {}

                    return@validate null
                }
            }
        }
        routing {
            route("my/test") {
                authenticate {
                    mount(server)
                }
            }
        }

        val pluginClient = KtorAndKotlinxClient(
            "my/test",
            RequestMaker(client.config {
                install(Auth) {
                    basic {
                        credentials {
                            BasicAuthCredentials(username = "test", password = "password")
                        }
                    }
                }
            }),
            KrosstalkClientSerialization(Json)
        )

        assertEquals(TestResponse(5, "a,b,c"), pluginClient.testEndpoint(TestRequest(listOf("a", "b", "c"), 2)))

        val directClient = KtorAndKotlinxClient(
            "my/test",
            RequestMaker(client) {
                basicAuth("test", "password")
            },
            KrosstalkClientSerialization(Json)
        )

        assertEquals(TestResponse(5, "a,b,c"), directClient.testEndpoint(TestRequest(listOf("a", "b", "c"), 2)))
    }

    @Test
    fun `works with compression`() = testApplication {
        install(Compression) {
            gzip {
                matchContentType(ContentType.Any)
                minimumSize(0)
                priority = 1.0
            }
            deflate {
                priority = 0.9
            }
        }

        routing {
            route("my/test") {
                mount(server) {
                    assertEquals("gzip;q=1.0,deflate;q=0.9", call.request.acceptEncoding())
                }
            }
        }

        val client = KtorAndKotlinxClient(
            "my/test",
            RequestMaker(
                client.config {
                    install(ContentEncoding) {
                        gzip(1.0F)
                        deflate(0.9F)
                    }
                },
                {
                    assertEquals("gzip", it.headers["Content-Encoding"])
                }
            ),
            KrosstalkClientSerialization(Json)
        )

        assertEquals(TestResponse(5, "a,b,c"), client.testEndpoint(TestRequest(listOf("a", "b", "c"), 2)))
    }

}