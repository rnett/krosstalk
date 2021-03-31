package com.rnett.krosstalk.fullstack_test

import com.rnett.krosstalk.client.invoke
import com.rnett.krosstalk.ktor.client.BasicCredentials
import io.ktor.client.utils.EmptyContent
import io.ktor.http.HttpMethod
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

//TODO full set of tests
class Tests {

    @Test
    fun testBasic() = GlobalScope.promise {
        assertEquals(listOf("t", "t"), basicTest(Data(2, "t")))
        assertEquals("/krosstalk/basicTest_7e16qc", lastUrl)
    }.asDynamic()

    @Test
    fun testBasicEndpoint() = GlobalScope.promise {
        assertEquals("aaa", basicEndpointTest(3, "a"))
        assertEquals("/base/basicEndpointTest_ix3eu8/test", lastUrl)
    }

    @Test
    fun testEndpointMethod() = GlobalScope.promise {
        assertEquals(10, endpointMethodTest(5, 5))
        assertEquals("/krosstalk/endpointMethodTest_mg7fyz", lastUrl)
        assertEquals(HttpMethod.Put, lastHttpMethod)
    }

    @Test
    fun testEndpointContentType() = GlobalScope.promise {
        assertEquals(10, endpointContentTypeTest(5, 5))
        assertEquals("/krosstalk/endpointContentTypeTest_xd5frk", lastUrl)
    }

    @Test
    fun testEmptyGet() = GlobalScope.promise {
        assertEquals("Hello World!", emptyGet())
        assertEquals("/krosstalk/emptyGet_rjifj2", lastUrl)
        assertEquals(EmptyContent, lastBody)
        assertEquals(HttpMethod.Get, lastHttpMethod)
    }

    @Test
    fun testParamEndpointNoMinimize() = GlobalScope.promise {
        assertEquals(4 * 2 * 3 * 5, paramEndpointNoMinimize(4, 2, 3, 5))
        assertEquals("/krosstalk/paramEndpointNoMinimize_45pvl2/a/04/b/02?c=03&d=05", lastUrl)
    }

    @Test
    fun testOptionalEndpointNoMinimize() = GlobalScope.promise {
        assertEquals(null, optionalEndpointNoMinimize(2, null))
        assertEquals("/krosstalk/optionalEndpointNoMinimize_a7ntdk/n/02", lastUrl)

        assertEquals("bb", optionalEndpointNoMinimize(2, "b"))
        assertEquals("/krosstalk/optionalEndpointNoMinimize_a7ntdk/n/02/s/6162", lastUrl)
    }

    @Test
    fun testOptionalEndpointQueryParamsNoMinimize() = GlobalScope.promise {
        assertEquals(null, optionalEndpointQueryParamsNoMinimize(2, null))
        assertEquals("/krosstalk/optionalEndpointQueryParamsNoMinimize_ar4rh2?n=02", lastUrl)

        assertEquals("bb", optionalEndpointQueryParamsNoMinimize(2, "b"))
        assertEquals("/krosstalk/optionalEndpointQueryParamsNoMinimize_ar4rh2?n=02&s=6162", lastUrl)
    }

    @Test
    fun testParamEndpointMinimize() = GlobalScope.promise {
        assertEquals(4 * 2 * 3 * 5, paramEndpointMinimize(4, 2, 3, 5))
        assertEquals("/krosstalk/paramEndpointMinimize_r03fwr/a/04/b/02?c=03&d=05", lastUrl)
        assertEquals(EmptyContent, lastBody)
    }

    @Test
    fun testOptionalEndpointMinimize() = GlobalScope.promise {
        assertEquals(null, optionalEndpointMinimize(2, null))
        assertEquals("/krosstalk/optionalEndpointMinimize_wrk2cn/n/02", lastUrl)
        assertEquals(EmptyContent, lastBody)

        assertEquals("bb", optionalEndpointMinimize(2, "b"))
        assertEquals("/krosstalk/optionalEndpointMinimize_wrk2cn/n/02/s/6162", lastUrl)
        assertEquals(EmptyContent, lastBody)

        Throwable::class.isInstance(2)
    }

    @Test
    fun testOptionalEndpointQueryParamsMinimize() = GlobalScope.promise {
        assertEquals(null, optionalEndpointQueryParamsMinimize(2, null))
        assertEquals("/krosstalk/optionalEndpointQueryParamsMinimize_crypeu?n=02", lastUrl)
        assertEquals(EmptyContent, lastBody)

        assertEquals("bb", optionalEndpointQueryParamsMinimize(2, "b"))
        assertEquals("/krosstalk/optionalEndpointQueryParamsMinimize_crypeu?n=02&s=6162", lastUrl)
        assertEquals(EmptyContent, lastBody)
    }

    @Test
    fun testPartialMinimize() = GlobalScope.promise {
        assertEquals(null, partialMinimize(2, null))
        assertEquals("/krosstalk/partialMinimize_wxy3f3?n=02", lastUrl)
        assertEquals(EmptyContent, lastBody)

        assertEquals("bb", partialMinimize(2, "b"))
        assertEquals("/krosstalk/partialMinimize_wxy3f3?n=02", lastUrl)
        assertNotEquals(EmptyContent, lastBody)
    }

    @Test
    fun testWithResult() = GlobalScope.promise {
        testKrosstalkResultMatching()
    }

    @Test
    fun testWithResultCatching() = GlobalScope.promise {
        testKrosstalkResultCatchingMatching()
    }

    @Test
    fun testOverload() = GlobalScope.promise {
        assertEquals("2", withOverload(2))
        assertEquals(2, withOverload("2"))
    }

    @Test
    fun testAuth() = GlobalScope.promise {
        assertEquals("username", withAuth(2, MyKrosstalk.Auth(BasicCredentials("username", "password"))))
    }

    @Test
    fun testOptionalAuth() = GlobalScope.promise {
        assertEquals("username", withOptionalAuth(MyKrosstalk.Auth(BasicCredentials("username", "password"))))
        assertEquals(null, withOptionalAuth(null))
    }

}