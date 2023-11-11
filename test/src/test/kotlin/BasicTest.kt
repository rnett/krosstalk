package com.rnett.krosstalk.test

import com.rnett.krosstalk.client.KrosstalkClientSerialization
import com.rnett.krosstalk.client.RequestMaker
import com.rnett.krosstalk.server.KrosstalkServerSerialization
import com.rnett.krosstalk.server.mount
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import kotlin.properties.Delegates
import kotlin.test.assertEquals

class BasicTest {

    @Test
    fun `client functions`() = runTest {

        val requestMaker = mockk<RequestMaker>()
        val serialization = mockk<KrosstalkClientSerialization>()
        val method = BasicKrosstalk.SPEC.method("add")

        justRun { serialization.initializeForSpec(any()) }
        coEvery { requestMaker.makeRequest("test/add", "2+2".encodeToByteArray()) } returns "4".encodeToByteArray()
        every {
            serialization.serializeArguments(
                method,
                mapOf(
                    "a" to 2,
                    "b" to 2
                )
            )
        } returns "2+2".encodeToByteArray()
        every { serialization.deserializeReturnValue(method, "4".encodeToByteArray()) } returns "4"

        val client = BasicKrosstalk.client("test", requestMaker, serialization)

        assertEquals("4", client.add(2, 2))

        verify {
            serialization.initializeForSpec(BasicKrosstalk.SPEC)
        }
    }

    @Test
    fun `server functions`() = runTest {

        val serialization = mockk<KrosstalkServerSerialization>()
        val method = BasicKrosstalk.SPEC.method("add")

        justRun { serialization.initializeForSpec(any()) }
        every {
            serialization.deserializeArguments(
                method,
                "2+2".encodeToByteArray()
            )
        } returns mapOf("a" to 2, "b" to 2)
        every { serialization.serializeReturnValue(method, "4") } returns "4".encodeToByteArray()

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

        verify {
            serialization.initializeForSpec(BasicKrosstalk.SPEC)
        }
    }
}