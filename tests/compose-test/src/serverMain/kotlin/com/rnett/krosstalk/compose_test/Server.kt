package com.rnett.krosstalk.compose_test

import com.rnett.krosstalk.KrosstalkPluginApi

@OptIn(KrosstalkPluginApi::class)
fun main() {
    println(TodoKrosstalk.methods)
//    embeddedServer(CIO, 8082, "localhost") {
//        install(CORS) {
//            anyHost()
//        }
//        install(CallLogging) {
//            level = Level.DEBUG
//        }
//
//        routing {
//            TodoKrosstalk.defineKtor(this)
//        }
//
//    }.start(true)
}