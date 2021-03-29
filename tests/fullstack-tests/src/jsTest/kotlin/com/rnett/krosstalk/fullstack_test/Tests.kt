package com.rnett.krosstalk.fullstack_test

import com.rnett.krosstalk.fullstack_test.Data
import com.rnett.krosstalk.fullstack_test.lastBody
import com.rnett.krosstalk.fullstack_test.lastUrl
import io.ktor.client.utils.EmptyContent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

//TODO full set of tests
class Tests {

    @Test
    fun testBasic() = GlobalScope.promise {
        assertEquals(listOf("t", "t"), basicTest(Data(2, "t")))
        assertEquals("/krosstalk/basicTest", lastUrl)
    }.asDynamic()

    @Test
    fun testBasicEndpoint() = GlobalScope.promise {
        assertEquals("aaa", basicEndpointTest(3, "a"))
        assertEquals("/base/basicEndpointTest/test", lastUrl)
    }

    @Test
    fun testEndpointMethod() = GlobalScope.promise {
        assertEquals(10, endpointMethodTest(5, 5))
        assertEquals("/krosstalk/endpointMethodTest", lastUrl)
    }

    @Test
    fun testEmptyGet() = GlobalScope.promise {
        assertEquals("Hello World!", emptyGet())
        assertEquals("/krosstalk/emptyGet", lastUrl)
        assertEquals(EmptyContent, lastBody)
    }

    @Test
    fun testParamEndpointNoMinimize() = GlobalScope.promise {
        assertEquals(4 * 2 * 3 * 5, paramEndpointNoMinimize(4, 2, 3, 5))
        assertEquals("/krosstalk/paramEndpointNoMinimize/a/04/b/02?c=03&d=05", lastUrl)
    }

    @Test
    fun testOptionalEndpointNoMinimize() = GlobalScope.promise {
        assertEquals(null, optionalEndpointNoMinimize(2, null))
        assertEquals("/krosstalk/optionalEndpointNoMinimize/n/02", lastUrl)

        assertEquals("bb", optionalEndpointNoMinimize(2, "b"))
        assertEquals("/krosstalk/optionalEndpointNoMinimize/n/02/s/6162", lastUrl)
    }

    @Test
    fun testOptionalEndpointQueryParamsNoMinimize() = GlobalScope.promise {
        assertEquals(null, optionalEndpointQueryParamsNoMinimize(2, null))
        assertEquals("/krosstalk/optionalEndpointQueryParamsNoMinimize?n=02", lastUrl)

        assertEquals("bb", optionalEndpointQueryParamsNoMinimize(2, "b"))
        assertEquals("/krosstalk/optionalEndpointQueryParamsNoMinimize?n=02&s=6162", lastUrl)
    }

    @Test
    fun testParamEndpointMinimize() = GlobalScope.promise {
        assertEquals(4 * 2 * 3 * 5, paramEndpointMinimize(4, 2, 3, 5))
        assertEquals("/krosstalk/paramEndpointMinimize/a/04/b/02?c=03&d=05", lastUrl)
        assertEquals(EmptyContent, lastBody)
    }

    @Test
    fun testOptionalEndpointMinimize() = GlobalScope.promise {
        assertEquals(null, optionalEndpointMinimize(2, null))
        assertEquals("/krosstalk/optionalEndpointMinimize/n/02", lastUrl)
        assertEquals(EmptyContent, lastBody)

        assertEquals("bb", optionalEndpointMinimize(2, "b"))
        assertEquals("/krosstalk/optionalEndpointMinimize/n/02/s/6162", lastUrl)
        assertEquals(EmptyContent, lastBody)

        Throwable::class.isInstance(2)
    }

    @Test
    fun testOptionalEndpointQueryParamsMinimize() = GlobalScope.promise {
        assertEquals(null, optionalEndpointQueryParamsMinimize(2, null))
        assertEquals("/krosstalk/optionalEndpointQueryParamsMinimize?n=02", lastUrl)
        assertEquals(EmptyContent, lastBody)

        assertEquals("bb", optionalEndpointQueryParamsMinimize(2, "b"))
        assertEquals("/krosstalk/optionalEndpointQueryParamsMinimize?n=02&s=6162", lastUrl)
        assertEquals(EmptyContent, lastBody)
    }

    @Test
    fun testPartialMinimize() = GlobalScope.promise {
        assertEquals(null, partialMinimize(2, null))
        assertEquals("/krosstalk/partialMinimize?n=02", lastUrl)
        assertEquals(EmptyContent, lastBody)

        assertEquals("bb", partialMinimize(2, "b"))
        assertEquals("/krosstalk/partialMinimize?n=02", lastUrl)
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