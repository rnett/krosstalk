package com.rnett.krosstalk.native_test

import com.rnett.krosstalk.KrosstalkPluginApi
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class Tests {
    //TODO waiting on https://youtrack.jetbrains.com/issue/KT-46896
    @Test
    @OptIn(KrosstalkPluginApi::class)
    fun testPlugin() {
        assertEquals(1, TestKrosstalk.methods.size)
    }

    @Test
    fun testBasic() {
        runBlocking {
            assertEquals(Item(3, "test"), testBasic(3, "test"))
        }
    }
}