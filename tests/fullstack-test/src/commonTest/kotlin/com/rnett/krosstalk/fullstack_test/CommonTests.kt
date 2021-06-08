package com.rnett.krosstalk.fullstack_test

import com.rnett.krosstalk.result.KrosstalkHttpError
import com.rnett.krosstalk.result.KrosstalkServerException
import com.rnett.krosstalk.result.isHttpError
import com.rnett.krosstalk.result.isServerException
import com.rnett.krosstalk.result.valueOrNull
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

object CommonTests {

    suspend fun krosstalkResultMatching() {

        assertEquals(2, withResult(2).valueOrNull)

        val result = withResult(-2)

        assertTrue(result.isServerException())
        assertEquals("java.lang.IllegalStateException: Can't have n < 0", result.asStringNoStacktrace)
        assertNotNull(result.asStringWithStacktrace)
    }

    suspend fun krosstalkResultCatchingMatching() {

        assertEquals(2, withResultCatching(2).valueOrNull)

        val result = withResultCatching(-2)

        assertTrue(result.isHttpError())
        assertEquals(422, result.statusCode)
        assertEquals("Unprocessable Entity", result.statusCodeName)
        assertEquals("Can't have n < 0", result.message)
    }

    suspend fun krosstalkResultSuccessOrHttpError() {
        val result = withSuccessOrHttpError(2)

        assertEquals(4, result.valueOrNull)

        val errorResult = withSuccessOrHttpError(-2)
        assertTrue(errorResult.isHttpError())
        assertEquals(411, errorResult.statusCode)
        assertEquals("Negative n = -2", errorResult.message)
    }

    suspend fun krosstalkResultSuccessOrServerException(shouldHaveStackTrace: Boolean) {
        val result = withSuccessOrServerException(2)

        assertEquals(4, result.valueOrNull)

        val errorResult = withSuccessOrServerException(-2)
        assertTrue(errorResult.isServerException())
        assertEquals("java.lang.IllegalStateException", errorResult.className)
        assertEquals("Negative n = -2", errorResult.message)
        assertEquals(
            shouldHaveStackTrace,
            errorResult.asStringWithStacktrace != null,
            "Stack trace doesn't match presence setting: $shouldHaveStackTrace"
        )
    }

    suspend fun KrosstalkHttpError() {
        val errorResult = withHttpError(-2)
        assertTrue(errorResult.isHttpError())
        assertEquals(404, errorResult.statusCode)
        assertEquals("Hide and Seek", errorResult.message)
    }

    suspend fun nonKrosstalkHttpError() {
        assertEquals(4, withNonKrosstalkHttpError(2))
        try {
            withNonKrosstalkHttpError(-2)
            fail("No exception")
        } catch (e: KrosstalkHttpError) {
            assertEquals(411, e.httpError.statusCode)
            assertEquals("Negative n = -2", e.httpError.message)
        }
    }

    suspend fun nonKrosstalkServerException() {
        assertEquals(4, withNonKrosstalkServerException(2))
        try {
            withNonKrosstalkServerException(-2)
            fail("No exception")
        } catch (e: KrosstalkServerException) {
            assertEquals("java.lang.IllegalStateException", e.exception.className)
            assertEquals("Negative n = -2", e.exception.message)
        }
    }

}