package com.rnett.krosstalk.fullstack_test

import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class JvmTests {
    @Test
    fun testServerExplicitResultMatching() = runBlocking {
        testKrosstalkResultMatching()
    }

    @Test
    fun testServerExplicitResultCatchingMatching() = runBlocking {
        testKrosstalkResultCatchingMatching()
    }
}