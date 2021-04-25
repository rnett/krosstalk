package com.rnett.krosstalk.ping

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class Tests {
    @Test
    fun testPing() = runBlocking {
        val r = ping(10, call = false)
        assertEquals(true, r)
    }
}