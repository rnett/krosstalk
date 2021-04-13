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
    assertEquals(422, result.httpErrorOrNull!!.statusCode)
    //TODO re-enable this once I get rid of compiler wrapping
//    assertEquals("Unprocessable Entity", result.httpErrorOrNull!!.statusCodeName)
    assertEquals("Can't have n < 0", result.httpErrorOrNull!!.message)

    assertEquals(2, withResultCatching(2).valueOrNull)
}