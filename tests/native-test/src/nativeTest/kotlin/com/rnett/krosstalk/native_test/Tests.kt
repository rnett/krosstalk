package com.rnett.krosstalk.native_test

import com.rnett.krosstalk.KrosstalkPluginApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class Tests {
    @Test
    @OptIn(KrosstalkPluginApi::class)
    fun testPlugin() = runTest {
        assertEquals(1, TestKrosstalk.methods.size)
    }

    @Test
    fun testBasic() = runTest {
        assertEquals(Item(3, "test"), testBasic(3, "test"))
    }
}