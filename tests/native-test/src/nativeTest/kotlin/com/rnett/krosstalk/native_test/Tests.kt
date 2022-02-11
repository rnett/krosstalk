package com.rnett.krosstalk.native_test

import com.rnett.krosstalk.KrosstalkPluginApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class Tests {
    @Test
    @OptIn(KrosstalkPluginApi::class)
    @Ignore
    fun testPlugin() = runTest {
        assertEquals(1, TestKrosstalk.methods.size)
    }

    @Test
    @Ignore
    fun testBasic() = runTest {
        assertEquals(Item(3, "test"), testBasic(3, "test"))
    }
}