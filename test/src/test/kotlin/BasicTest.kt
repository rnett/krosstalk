package com.rnett.krosstalk.test

import com.rnett.krosstalk.client.KrosstalkClientSerialization
import com.rnett.krosstalk.client.RequestMaker
import com.rnett.krosstalk.metadata.Argument
import com.rnett.krosstalk.metadata.ParameterType
import com.rnett.krosstalk.server.KrosstalkServerSerialization
import com.rnett.krosstalk.server.mount
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import kotlin.properties.Delegates
import kotlin.reflect.typeOf
import kotlin.test.assertEquals

class BasicTest {

    @Test
    fun `client functions`() = runTest {

        val requestMaker = mockk<RequestMaker>()
        val serialization = mockk<KrosstalkClientSerialization>()

        coEvery { requestMaker.makeRequest("test/add", "2+2".encodeToByteArray()) } returns "4".encodeToByteArray()
        every {
            serialization.serialize(
                mapOf(
                    "a" to Argument(typeOf<Int>(), 2),
                    "b" to Argument(typeOf<Int>(), 2)
                )
            )
        } returns "2+2".encodeToByteArray()
        every { serialization.deserialize("4".encodeToByteArray(), typeOf<String>()) } returns "4"

        val client = BasicKrosstalk.client("test", requestMaker, serialization)

        assertEquals("4", client.add(2, 2))
    }

    @Test
    fun `server functions`() = runTest {

        val serialization = mockk<KrosstalkServerSerialization>()

        every {
            serialization.deserialize(
                mapOf(
                    "a" to ParameterType(typeOf<Int>()),
                    "b" to ParameterType(typeOf<Int>())
                ), "2+2".encodeToByteArray()
            )
        } returns mapOf("a" to 2, "b" to 2)
        every { serialization.serialize("4", typeOf<String>()) } returns "4".encodeToByteArray()

        val server = object : BasicKrosstalkServer(serialization) {
            override suspend fun add(a: Int, b: Int): String {
                return (a + b).toString()
            }
        }

        var invoker: (suspend (ByteArray) -> ByteArray) by Delegates.notNull()

        server.mount { subPath, invoke ->
            if (subPath == "add")
                invoker = invoke
        }

        assertArrayEquals("4".encodeToByteArray(), invoker.invoke("2+2".encodeToByteArray()))

    }
}