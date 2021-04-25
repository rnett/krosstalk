package com.rnett.krosstalk.pong

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class Tests {
    @Test
    fun testPong() = runBlocking {
        assertEquals(true, pong(10, call = false))
    }
}