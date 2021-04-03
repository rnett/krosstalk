package com.rnett.krosstalk.fullstack_test

import com.rnett.krosstalk.ServerDefault
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
        assertEquals(4 * 2 * 3 * 5, paramEndpoint(4, 2, 3, 5))
        assertEquals("/krosstalk/paramEndpoint_nxd1nq/a/04/b/02?c=03&d=05", lastUrl)
    }

    @Test
    fun testOptionalEndpointNoMinimize() = GlobalScope.promise {
        assertEquals(null, optionalEndpoint(2, null))
        assertEquals("/krosstalk/optionalEndpoint_h9r8ve/n/02", lastUrl)

        assertEquals("bb", optionalEndpoint(2, "b"))
        assertEquals("/krosstalk/optionalEndpoint_h9r8ve/n/02/s/6162", lastUrl)
    }

    @Test
    fun testOptionalEndpointQueryParamsNoMinimize() = GlobalScope.promise {
        assertEquals(null, optionalEndpointQueryParams(2, null))
        assertEquals("/krosstalk/optionalEndpointQueryParams_krs4ra?n=02", lastUrl)

        assertEquals("bb", optionalEndpointQueryParams(2, "b"))
        assertEquals("/krosstalk/optionalEndpointQueryParams_krs4ra?n=02&s=6162", lastUrl)
    }

    @Test
    fun testParamEndpointMinimize() = GlobalScope.promise {
        assertEquals(4 * 2 * 3 * 5, paramEndpointGet(4, 2, 3, 5))
        assertEquals("/krosstalk/paramEndpointGet_wwiikt/a/04/b/02?c=03&d=05", lastUrl)
        assertEquals(EmptyContent, lastBody)
    }

    @Test
    fun testOptionalEndpointMinimize() = GlobalScope.promise {
        assertEquals(null, optionalEndpointGet(2, null))
        assertEquals("/krosstalk/optionalEndpointGet_imq958/n/02", lastUrl)
        assertEquals(EmptyContent, lastBody)

        assertEquals("bb", optionalEndpointGet(2, "b"))
        assertEquals("/krosstalk/optionalEndpointGet_imq958/n/02/s/6162", lastUrl)
        assertEquals(EmptyContent, lastBody)

        Throwable::class.isInstance(2)
    }

    @Test
    fun testOptionalEndpointQueryParamsMinimize() = GlobalScope.promise {
        assertEquals(null, optionalEndpointQueryParamsGet(2, null))
        assertEquals("/krosstalk/optionalEndpointQueryParamsGet_34mu26?n=02", lastUrl)
        assertEquals(EmptyContent, lastBody)

        assertEquals("bb", optionalEndpointQueryParamsGet(2, "b"))
        assertEquals("/krosstalk/optionalEndpointQueryParamsGet_34mu26?n=02&s=6162", lastUrl)
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

    @Test
    fun testOptionalReceiver() = GlobalScope.promise {
        assertEquals("ddd", 3.withOptionalReceiver("d"))
        assertEquals("dd", null.withOptionalReceiver("d"))
        assertEquals("aaa", 3.withOptionalReceiver(null))
        assertEquals("aa", null.withOptionalReceiver(null))
    }

    @Test
    fun testOptionalWithClientSideDefault() = GlobalScope.promise {
        assertEquals(6, withOptionalDefault(2, 3))
        assertEquals(8, withOptionalDefault())
        assertEquals(40, withOptionalDefault(10))
        assertEquals(20, withOptionalDefault(b = 10))
        assertEquals(0, withOptionalDefault(b = null))
    }

    @Test
    fun testKrosstalkOptionalServerDefault() = GlobalScope.promise {
        assertEquals(6, withServerDefault(3, ServerDefault { 2 }))
        assertEquals("/krosstalk/withServerDefault_y1hr3w/a/03/b/02", lastUrl)

        assertEquals(12, withServerDefault(3))
        assertEquals("/krosstalk/withServerDefault_y1hr3w/a/03", lastUrl)
    }
}