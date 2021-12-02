package com.rnett.krosstalk.ping

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class Tests {
    @Test
    fun testPing() = runTest {
        val r = ping(10, call = false)
        assertEquals(true, r)
    }
}