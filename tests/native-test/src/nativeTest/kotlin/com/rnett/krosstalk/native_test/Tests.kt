package com.rnett.krosstalk.native_test

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import com.rnett.krosstalk.KrosstalkPluginApi

class Tests{
    //TODO waiting on https://youtrack.jetbrains.com/issue/KT-46896
    @Test
    @OptIn(KrosstalkPluginApi::class)
    fun testPlugin(){
        assertEquals(1, TestKrosstalk.methods.size)
    }
    @Test
    fun testBasic(){
        runBlocking {
            assertEquals(3, 3)
//            assertEquals(Item(3, "test"), testBasic(4, "test"))
        }
    }
}