package com.rnett.krosstalk.fullstack_test

import kotlin.test.assertEquals
import kotlin.test.assertTrue


suspend fun testKrosstalkResultMatching() {
    val result = withResult(-2)

    println(result)
    assertTrue(result.isServerException())
    assertEquals("java.lang.IllegalStateException: Can't have n < 0", result.serverExceptionOrNull?.asString)

    println("Result: " + withResult(2))
    assertEquals(2, withResult(2).valueOrNull)
}