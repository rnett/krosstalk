package com.rnett.krosstalk.client_test

import com.rnett.krosstalk.ktor.client.auth.invoke
import com.rnett.krosstalk.result.httpErrorOrNull
import com.rnett.krosstalk.result.valueOrNull
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
    fun testSetItems() = GlobalScope.promise{
        setItem(100, Item(20, "Test Item"))
        assertEquals(Item(20, "Test Item"), getItem(100).valueOrNull)
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
        assertEquals(Unit, getTestUnit("http://localhost:8081/inner/server/path"))
    }

    @Test
    fun testUnitServerUrlInCall() = GlobalScope.promise {
        assertEquals(Unit, with(WithServerUrl("http://localhost:8081/inner/server/path")) { getTestUnitCallServerUrl() })
    }

    @Test
    fun testAuthInCall() = GlobalScope.promise {
        assertEquals("user", getUserWithCallAuth("user", "pass").valueOrNull)
        assertEquals(401, getUserWithCallAuth("user", "password2").httpErrorOrNull?.statusCode)
    }

    @Test
    fun testBearerAuth() = GlobalScope.promise {
        assertEquals(10, tryBearer(MyKrosstalk.BearerAuth("testToken")))
    }
}