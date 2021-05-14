package com.rnett.krosstalk.fullstack_test

import com.rnett.krosstalk.result.KrosstalkHttpError
import com.rnett.krosstalk.result.valueOrNull
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class JvmTests {
    @Test
    fun testServerExplicitResultMatching() = runBlocking {
        CommonTests.krosstalkResultMatching()
    }

    @Test
    fun testServerExplicitResultCatchingMatching() = runBlocking {
        CommonTests.krosstalkResultCatchingMatching()
    }

    @Test
    fun testServerResultSuccessOrHttpError() = runBlocking {
        CommonTests.krosstalkResultSuccessOrHttpError()
    }

    @Test
    fun testServerResultSuccessOrServerException() = runBlocking {
        CommonTests.krosstalkResultSuccessOrServerException()
    }

    @Test
    fun testServerResultHttpError() = runBlocking {
        CommonTests.krosstalkResultHttpError()
    }

    @Test
    fun testNonKrosstalkHttpError() = runBlocking {
        CommonTests.nonKrosstalkHttpError()
    }

    @Test
    fun testNonKrosstalkServerException() = runBlocking {
        CommonTests.nonKrosstalkServerException()
    }

    @Test
    fun testNonKrosstalkUncaughtException() = runBlocking {
        assertEquals(4, withNonKrosstalkUncaughtException(2))
        try {
            withNonKrosstalkUncaughtException(-2)
            fail("No exception")
        } catch (e: IllegalStateException) {
            assertEquals("Negative n = -2", e.message)
        }
    }

    @Test
    fun testUncaughtExceptionOutsideKrosstalkResult() = runBlocking {
        assertEquals(4, withUncaughtExceptionOutsideKrosstalkResult(2).valueOrNull)
        try {
            withUncaughtExceptionOutsideKrosstalkResult(-2)
            fail("No exception")
        } catch (e: IllegalStateException) {
            assertEquals("Negative n = -2", e.message)
        }
    }

    @Test
    fun testHttpErrorOutsideKrosstalkResult() = runBlocking {
        assertEquals(4, withHttpErrorOutsideKrosstalkResult(2).valueOrNull)
        try {
            withHttpErrorOutsideKrosstalkResult(-2)
            fail("No exception")
        } catch (e: KrosstalkHttpError) {
            assertEquals(411, e.httpError.statusCode)
            assertEquals("Negative n = -2", e.httpError.message)
        }
    }
}