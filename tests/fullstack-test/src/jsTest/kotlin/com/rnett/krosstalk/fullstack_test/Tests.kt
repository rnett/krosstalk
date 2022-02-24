package com.rnett.krosstalk.fullstack_test

import com.rnett.krosstalk.ServerDefault
import com.rnett.krosstalk.headersOf
import com.rnett.krosstalk.ktor.client.auth.invoke
import com.rnett.krosstalk.result.KrosstalkHttpError
import com.rnett.krosstalk.result.KrosstalkUncaughtServerException
import com.rnett.krosstalk.result.httpErrorOrNull
import com.rnett.krosstalk.result.isServerException
import com.rnett.krosstalk.result.isSuccess
import com.rnett.krosstalk.result.serverExceptionOrNull
import com.rnett.krosstalk.result.valueOrNull
import io.ktor.client.utils.EmptyContent
import io.ktor.http.HttpMethod
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

@OptIn(DelicateCoroutinesApi::class)
class Tests {

    @Test
    fun testBasic() = runTest {
        assertEquals(listOf("t", "t"), basicTest(Data(2, "t")))
        assertEquals("/krosstalk/basicTest_7e16qc", lastUrl)
    }.asDynamic()

    @Test
    fun testBasicEndpoint() = runTest {
        assertEquals("aaa", basicEndpointTest(3, "a"))
        assertEquals("/base/basicEndpointTest_ix3eu8/test", lastUrl)
    }

    @Test
    fun testEndpointMethod() = runTest {
        assertEquals(10, endpointMethodTest(5, 5))
        assertEquals("/krosstalk/endpointMethodTest_mg7fyz", lastUrl)
        assertEquals(HttpMethod.Put, lastHttpMethod)
    }

    @Test
    fun testEndpointContentType() = runTest {
        assertEquals(10, endpointContentTypeTest(5, 5))
        assertEquals("/krosstalk/endpointContentTypeTest_xd5frk", lastUrl)
    }

    @Test
    fun testEmptyGet() = runTest {
        assertEquals("Hello World!", emptyGet())
        assertEquals("/krosstalk/emptyGet_rjifj2", lastUrl)
        assertEquals(EmptyContent, lastBody)
        assertEquals(HttpMethod.Get, lastHttpMethod)
    }

    @Test
    fun testParamEndpointNoMinimize() = runTest {
        assertEquals(4 * 2 * 3 * 5, paramEndpoint(4, 2, 3, 5))
        assertEquals("/krosstalk/paramEndpoint_nxd1nq/a/04/b/02?c=03&d=05", lastUrl)
    }

    @Test
    fun testOptionalEndpointNoMinimize() = runTest {
        assertEquals(null, optionalEndpoint(2, null))
        assertEquals("/krosstalk/optionalEndpoint_h9r8ve/n/02", lastUrl)

        assertEquals("bb", optionalEndpoint(2, "b"))
        assertEquals("/krosstalk/optionalEndpoint_h9r8ve/n/02/s/6162", lastUrl)
    }

    @Test
    fun testOptionalEndpointQueryParamsNoMinimize() = runTest {
        assertEquals(null, optionalEndpointQueryParams(2, null))
        assertEquals("/krosstalk/optionalEndpointQueryParams_krs4ra?n=02", lastUrl)

        assertEquals("bb", optionalEndpointQueryParams(2, "b"))
        assertEquals("/krosstalk/optionalEndpointQueryParams_krs4ra?n=02&s=6162", lastUrl)
    }

    @Test
    fun testParamEndpointMinimize() = runTest {
        assertEquals(4 * 2 * 3 * 5, paramEndpointGet(4, 2, 3, 5))
        assertEquals("/krosstalk/paramEndpointGet_wwiikt/a/04/b/02?c=03&d=05", lastUrl)
        assertEquals(EmptyContent, lastBody)
    }

    @Test
    fun testOptionalEndpointMinimize() = runTest {
        assertEquals(null, optionalEndpointGet(2, null))
        assertEquals("/krosstalk/optionalEndpointGet_imq958/n/02", lastUrl)
        assertEquals(EmptyContent, lastBody)

        assertEquals("bb", optionalEndpointGet(2, "b"))
        assertEquals("/krosstalk/optionalEndpointGet_imq958/n/02/s/6162", lastUrl)
        assertEquals(EmptyContent, lastBody)

        Throwable::class.isInstance(2)
    }

    @Test
    fun testOptionalEndpointQueryParamsMinimize() = runTest {
        assertEquals(null, optionalEndpointQueryParamsGet(2, null))
        assertEquals("/krosstalk/optionalEndpointQueryParamsGet_34mu26?n=02", lastUrl)
        assertEquals(EmptyContent, lastBody)

        assertEquals("bb", optionalEndpointQueryParamsGet(2, "b"))
        assertEquals("/krosstalk/optionalEndpointQueryParamsGet_34mu26?n=02&s=6162", lastUrl)
        assertEquals(EmptyContent, lastBody)
    }

    @Test
    fun testPartialMinimize() = runTest {
        assertEquals(null, partialMinimize(2, null))
        assertEquals("/krosstalk/partialMinimize_wxy3f3?n=02", lastUrl)
        assertEquals(EmptyContent, lastBody)

        assertEquals("bb", partialMinimize(2, "b"))
        assertEquals("/krosstalk/partialMinimize_wxy3f3?n=02", lastUrl)
        assertNotEquals(EmptyContent, lastBody)
    }

    @Test
    fun testWithResult() = runTest {
        CommonTests.krosstalkResultMatching()
        assertEquals(500, lastStatusCode)
    }

    @Test
    fun testWithResultCatching() = runTest {
        CommonTests.krosstalkResultCatchingMatching()
        assertEquals(422, lastStatusCode)
    }

    @Test
    fun testOverload() = runTest {
        assertEquals("2", withOverload(2))
        assertEquals(2, withOverload("2"))
    }

    @Test
    fun testAuth() = runTest {
        assertEquals("username", withAuth(2, MyKrosstalk.Auth("username", "password")))
    }

    @Test
    fun testOptionalAuth() = runTest {
        assertEquals("username", withOptionalAuth(MyKrosstalk.Auth("username", "password")))
        assertEquals(null, withOptionalAuth(null))
    }

    @Test
    fun testOptionalReceiver() = runTest {
        assertEquals("ddd", 3.withOptionalReceiver("d"))
        assertEquals("dd", null.withOptionalReceiver("d"))
        assertEquals("aaa", 3.withOptionalReceiver(null))
        assertEquals("aa", null.withOptionalReceiver(null))
    }

    @Test
    fun testOptionalWithDefault() = runTest {
        assertEquals(6, withOptionalDefault(2, 3))
        assertEquals(8, withOptionalDefault())
        assertEquals(40, withOptionalDefault(10))
        assertEquals(20, withOptionalDefault(b = 10))
        assertEquals(0, withOptionalDefault(b = null))
    }

    @Test
    fun testServerDefault() = runTest {
        assertEquals(6, withServerDefault(3, ServerDefault { 2 }))
        assertEquals("/krosstalk/withServerDefault_y1hr3w/a/03/b/02", lastUrl)

        assertEquals(12, withServerDefault(3))
        assertEquals("/krosstalk/withServerDefault_y1hr3w/a/03", lastUrl)
    }

    @Test
    fun testObjectNoPassingReceiver() = runTest {
        assertEquals(10, ExpectObject.withExpectObjectParam())
    }

    @Test
    fun testObjectNoPassing() = runTest {
        assertEquals(10, withExpectObjectValueParam(ExpectObject))
    }

    @Test
    fun testObjectPassing() = runTest {
        assertEquals(3, withPassedExpectObjectValueParam(SerializableObject))
    }

    @Test
    fun testUnitReturn() = runTest {
        //TODO test response is actually empty
        assertEquals(Unit, withUnitReturn("s"))
    }

    @Test
    fun testObjectReturn() = runTest {
        assertEquals(ExpectObject, withObjectReturn("s"))
    }

    @Test
    fun testPassedObjectReturn() = runTest {
        assertEquals(SerializableObject, withPassedObjectReturn("s"))
    }

    @Test
    fun testDifferentObjectPassing() = runTest {
        assertEquals(ExpectObject, withDifferentPassing(SerializableObject))
    }

    @Test
    fun testWithHeaders() = runTest {
        val result = withHeadersBasic(4)
        assertEquals("4", result.value)
        assertEquals(listOf("value"), result.headers["test"])
    }

    @Test
    fun testWithHeadersOutsideResult() = runTest {
        val result = withHeadersOutsideResult(4)

        assertEquals("4", result.value.valueOrNull)
        assertEquals(listOf("value"), result.headers["test"])

        val failure = withHeadersOutsideResult(-4)
        assertEquals(422, failure.value.httpErrorOrNull?.statusCode)
    }

    @Test
    fun testWithHeadersInsideResult() = runTest {
        val result = withHeadersInsideResult(4)

        assertEquals("4", result.valueOrNull!!.value)
        assertEquals(listOf("value"), result.valueOrNull!!.headers["test"])

        val failure = withHeadersInsideResult(-4)
        assertEquals(422, failure.httpErrorOrNull?.statusCode)
    }

    @Test
    fun testWithHeadersReturnObject() = runTest {
        val result = withHeadersReturnObject(10)

        assertEquals(ExpectObject, result.value)
        assertEquals("10", result.headers["value"]!![0])
    }

    @Test
    fun testRequestHeaders() = runTest {
        assertEquals(4, withRequestHeaders(2, headersOf("value" to "2")))
    }

    @Test
    fun testResultObject() = runTest {
        assertEquals(ExpectObject, withResultObject(2).valueOrNull)
        assertNotNull(withResultObject(-2).serverExceptionOrNull)
    }

    @Test
    fun testSuccessOrHttpError() = runTest {
        CommonTests.krosstalkResultSuccessOrHttpError()
        assertEquals(411, lastStatusCode)
    }

    @Test
    fun testSuccessOrServerException() = runTest {
        CommonTests.krosstalkResultSuccessOrServerException(false)
        assertEquals(500, lastStatusCode)
    }

    @Test
    fun testHttpError() = runTest {
        CommonTests.KrosstalkHttpError()
        assertEquals(404, lastStatusCode)
    }

    @Test
    fun testHttpErrorWithHeaders() = runTest {
        val result = withHttpErrorWithHeaders(2)
        assertEquals("test3", result.headers["test"]?.firstOrNull())
        assertEquals(416, result.value.httpErrorOrNull?.statusCode)
        assertEquals(416, lastStatusCode)
        assertEquals("Test", result.value.httpErrorOrNull?.message)
    }

    @Test
    fun testSuccessOrServerExceptionWithHeaders() = runTest {
        val result = withSuccessOrServerExceptionWithHeaders(2)
        assertTrue(result.isSuccess())
        assertEquals("test2", result.valueOrNull?.headers?.get("test")?.firstOrNull())
        assertEquals(4, result.valueOrNull?.value)

        val errorResult = withSuccessOrServerExceptionWithHeaders(-2)
        assertTrue(errorResult.isServerException())
        assertEquals("IllegalStateException", errorResult.classSimpleName)
        assertEquals(500, lastStatusCode)
    }

    @Test
    fun testNonKrosstalkHttpError() = runTest {
        CommonTests.nonKrosstalkHttpError()
        assertEquals(411, lastStatusCode)
    }

    @Test
    fun testNonKrosstalkServerException() = runTest {
        CommonTests.nonKrosstalkServerException()
        assertEquals(500, lastStatusCode)
    }

    @Test
    fun testNonKrosstalkUncaughtException() = runTest {
        assertEquals(4, withNonKrosstalkUncaughtException(2))
        try {
            withNonKrosstalkUncaughtException(-2)
            fail("No exception")
        } catch (e: KrosstalkUncaughtServerException) {
            assertEquals("java.lang.IllegalStateException", e.exception.className)
            assertEquals("Negative n = -2", e.exception.message)
        }
        assertEquals(500, lastStatusCode)
    }

    @Test
    fun testUncaughtExceptionOutsideKrosstalkResult() = runTest {
        assertEquals(4, withUncaughtExceptionOutsideKrosstalkResult(2).valueOrNull)
        try {
            withUncaughtExceptionOutsideKrosstalkResult(-2)
            fail("No exception")
        } catch (e: KrosstalkUncaughtServerException) {
            assertEquals("java.lang.IllegalStateException", e.exception.className)
            assertEquals("Negative n = -2", e.exception.message)
        }
        assertEquals(500, lastStatusCode)
    }

    @Test
    fun testHttpErrorOutsideKrosstalkResult() = runTest {
        assertEquals(4, withHttpErrorOutsideKrosstalkResult(2).valueOrNull)
        try {
            withHttpErrorOutsideKrosstalkResult(-2)
            fail("No exception")
        } catch (e: KrosstalkHttpError) {
            assertEquals(411, e.httpError.statusCode)
            assertEquals("Negative n = -2", e.httpError.message)
        }
        assertEquals(411, lastStatusCode)
    }

    @Test
    fun testHeadersInCall() = runTest {
        assertEquals("", withRequestHeadersInCall())
        assertEquals("2", lastHeaders?.get("test")?.firstOrNull())
    }

    @Test
    fun testHeadersInCallAndParam() = runTest {
        val result = withRequestHeadersInCallAndParam(headersOf("b" to "2"), "a" to "b")
        assertEquals("call", result.first)
        assertEquals("2", result.second)
    }

    @Test
    fun testIgnored() = runTest {
        assertEquals("test", withIgnored())
        assertEquals("test", withIgnored("2"))
    }

    @Test
    fun testIgnoredDependentDefault() = runTest {
        assertEquals("testIgnore" to "testIgnore2", withIgnoredDependentDefault("test"))
    }

    @Test
    fun testDependentServerDefault() = runTest {
        assertEquals("2", withDependentServerDefault(2))
    }

    @Test
    fun testContextSerializable() = runTest {
        assertEquals(4, withContextSerializable(ContextSerializable(4)))
    }
}