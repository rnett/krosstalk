package com.rnett.krosstalk.pong

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class Tests {
    @Test
    fun testPong() = runTest {
        assertEquals(true, pong(10, call = false))
    }
}