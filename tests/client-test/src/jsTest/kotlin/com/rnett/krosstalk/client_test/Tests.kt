package com.rnett.krosstalk.client_test

import com.rnett.krosstalk.ktor.client.auth.invoke
import com.rnett.krosstalk.result.httpErrorOrNull
import com.rnett.krosstalk.result.valueOrNull
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class Tests {
    @Test
    fun testBasic() = runTest {
        assertEquals(List(20) { it }, itemIds())
    }

    @Test
    fun testSetItems() = runTest {
        setItem(100, Item(20, "Test Item"))
        assertEquals(Item(20, "Test Item"), getItem(100).valueOrNull)
    }

    @Test
    fun testKrosstalkResult() = runTest {
        assertEquals(Item(2, "Item 2"), getItem(2).valueOrNull)
        assertEquals(404, getItem(30).httpErrorOrNull?.statusCode)
    }

    @Test
    fun testAuth() = runTest {
        assertEquals("user", getUser(MyKrosstalk.Auth("user", "pass")).valueOrNull)
        assertEquals(401, getUser(MyKrosstalk.Auth("user", "password2")).httpErrorOrNull?.statusCode)
    }

    @Test
    fun testUnitServerUrl() = runTest {
        assertEquals(Unit, getTestUnit("http://localhost:8081/inner/server/path"))
    }

    @Test
    fun testUnitServerUrlInCall() = runTest {
        assertEquals(Unit, with(WithServerUrl("http://localhost:8081/inner/server/path")) { getTestUnitCallServerUrl() })
    }

    @Test
    fun testAuthInCall() = runTest {
        assertEquals("user", getUserWithCallAuth("user", "pass").valueOrNull)
        assertEquals(401, getUserWithCallAuth("user", "password2").httpErrorOrNull?.statusCode)
    }

    @Test
    fun testBearerAuth() = runTest {
        assertEquals(10, tryBearer(MyKrosstalk.BearerAuth("testToken")))
    }
}