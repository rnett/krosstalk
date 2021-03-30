package com.rnett.krosstalk.fullstack_test

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
        assertEquals("/krosstalk/basicTest_wto63g", lastUrl)
    }.asDynamic()

    @Test
    fun testBasicEndpoint() = GlobalScope.promise {
        assertEquals("aaa", basicEndpointTest(3, "a"))
        assertEquals("/base/basicEndpointTest_kk4egg/test", lastUrl)
    }

    @Test
    fun testEndpointMethod() = GlobalScope.promise {
        assertEquals(10, endpointMethodTest(5, 5))
        assertEquals("/krosstalk/endpointMethodTest_99264p", lastUrl)
        assertEquals(HttpMethod.Put, lastHttpMethod)
    }

    @Test
    fun testEndpointContentType() = GlobalScope.promise {
        assertEquals(10, endpointContentTypeTest(5, 5))
        assertEquals("/krosstalk/endpointContentTypeTest_99264p", lastUrl)
    }

    @Test
    fun testEmptyGet() = GlobalScope.promise {
        assertEquals("Hello World!", emptyGet())
        assertEquals("/krosstalk/emptyGet_0", lastUrl)
        assertEquals(EmptyContent, lastBody)
        assertEquals(HttpMethod.Get, lastHttpMethod)
    }

    @Test
    fun testParamEndpointNoMinimize() = GlobalScope.promise {
        assertEquals(4 * 2 * 3 * 5, paramEndpointNoMinimize(4, 2, 3, 5))
        assertEquals("/krosstalk/paramEndpointNoMinimize_ii49am/a/04/b/02?c=03&d=05", lastUrl)
    }

    @Test
    fun testOptionalEndpointNoMinimize() = GlobalScope.promise {
        assertEquals(null, optionalEndpointNoMinimize(2, null))
        assertEquals("/krosstalk/optionalEndpointNoMinimize_8tsh4t/n/02", lastUrl)

        assertEquals("bb", optionalEndpointNoMinimize(2, "b"))
        assertEquals("/krosstalk/optionalEndpointNoMinimize_8tsh4t/n/02/s/6162", lastUrl)
    }

    @Test
    fun testOptionalEndpointQueryParamsNoMinimize() = GlobalScope.promise {
        assertEquals(null, optionalEndpointQueryParamsNoMinimize(2, null))
        assertEquals("/krosstalk/optionalEndpointQueryParamsNoMinimize_8tsh4t?n=02", lastUrl)

        assertEquals("bb", optionalEndpointQueryParamsNoMinimize(2, "b"))
        assertEquals("/krosstalk/optionalEndpointQueryParamsNoMinimize_8tsh4t?n=02&s=6162", lastUrl)
    }

    @Test
    fun testParamEndpointMinimize() = GlobalScope.promise {
        assertEquals(4 * 2 * 3 * 5, paramEndpointMinimize(4, 2, 3, 5))
        assertEquals("/krosstalk/paramEndpointMinimize_ii49am/a/04/b/02?c=03&d=05", lastUrl)
        assertEquals(EmptyContent, lastBody)
    }

    @Test
    fun testOptionalEndpointMinimize() = GlobalScope.promise {
        assertEquals(null, optionalEndpointMinimize(2, null))
        assertEquals("/krosstalk/optionalEndpointMinimize_8tsh4t/n/02", lastUrl)
        assertEquals(EmptyContent, lastBody)

        assertEquals("bb", optionalEndpointMinimize(2, "b"))
        assertEquals("/krosstalk/optionalEndpointMinimize_8tsh4t/n/02/s/6162", lastUrl)
        assertEquals(EmptyContent, lastBody)

        Throwable::class.isInstance(2)
    }

    @Test
    fun testOptionalEndpointQueryParamsMinimize() = GlobalScope.promise {
        assertEquals(null, optionalEndpointQueryParamsMinimize(2, null))
        assertEquals("/krosstalk/optionalEndpointQueryParamsMinimize_8tsh4t?n=02", lastUrl)
        assertEquals(EmptyContent, lastBody)

        assertEquals("bb", optionalEndpointQueryParamsMinimize(2, "b"))
        assertEquals("/krosstalk/optionalEndpointQueryParamsMinimize_8tsh4t?n=02&s=6162", lastUrl)
        assertEquals(EmptyContent, lastBody)
    }

    @Test
    fun testPartialMinimize() = GlobalScope.promise {
        assertEquals(null, partialMinimize(2, null))
        assertEquals("/krosstalk/partialMinimize_8tsh4t?n=02", lastUrl)
        assertEquals(EmptyContent, lastBody)

        assertEquals("bb", partialMinimize(2, "b"))
        assertEquals("/krosstalk/partialMinimize_8tsh4t?n=02", lastUrl)
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


}