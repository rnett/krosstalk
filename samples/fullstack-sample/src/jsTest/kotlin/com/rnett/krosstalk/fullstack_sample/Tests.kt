package com.rnett.krosstalk.fullstack_sample

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import kotlin.test.Test
import kotlin.test.assertEquals

//TODO full set of tests
class Tests {

    @Test
    fun doTest() = GlobalScope.promise {
        assertEquals(listOf("t", "t"), doThing(Data(2, "t")))

    }

}