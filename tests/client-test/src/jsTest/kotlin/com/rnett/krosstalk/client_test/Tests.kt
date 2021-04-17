package com.rnett.krosstalk.client_test

import com.rnett.krosstalk.ktor.client.invoke
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import kotlin.test.Test
import kotlin.test.assertEquals

class Tests {
    @Test
    fun testBasic() = GlobalScope.promise {
        assertEquals(List(20) { it }, itemIds())
    }

    @Test
    fun testKrosstalkResult() = GlobalScope.promise {
        assertEquals(Item(2, "Item 2"), getItem(2).valueOrNull)
        assertEquals(404, getItem(30).httpErrorOrNull?.statusCode)
    }

    @Test
    fun testAuth() = GlobalScope.promise {
        assertEquals("user", getUser(MyKrosstalk.Auth("user", "pass")).valueOrNull)
        assertEquals(401, getUser(MyKrosstalk.Auth("user", "password2")).httpErrorOrNull?.statusCode)
    }

    @Test
    fun testUnitServerUrl() = GlobalScope.promise {
        assertEquals(Unit, getTestUnit("http://localhost:8080/inner/server/path"))
    }
}