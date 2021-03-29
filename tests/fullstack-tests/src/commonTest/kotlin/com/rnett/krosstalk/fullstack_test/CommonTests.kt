package com.rnett.krosstalk.fullstack_test

import kotlin.test.assertEquals
import kotlin.test.assertTrue


suspend fun testKrosstalkResultMatching() {
    val result = withResult(-2)

    assertTrue(result.isServerException())
    assertEquals("java.lang.IllegalStateException: Can't have n < 0", result.serverExceptionOrNull?.asString)

    assertEquals(2, withResult(2).valueOrNull)
}

suspend fun testKrosstalkResultCatchingMatching() {
    val result = withResultCatching(-2)

    assertTrue(result.isHttpError())
    assertEquals(505, result.httpErrorOrNull!!.responseCode)
    assertEquals("Can't have n < 0", result.httpErrorOrNull!!.clientMessage)

    assertEquals(2, withResultCatching(2).valueOrNull)
}