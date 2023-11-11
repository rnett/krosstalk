package com.rnett.krosstalk.test

import com.rnett.krosstalk.Krosstalk

@Krosstalk
interface BasicKrosstalk {
    suspend fun add(a: Int, b: Int): String

    companion object
}
