package com.rnett.krosstalk.ping

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        GlobalScope.launch {
            ping(0)
        }
    }
}