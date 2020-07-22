package com.rnett.krosstalk

import kotlinx.serialization.Serializable

@Serializable
data class Data(val num: Int, val str: String)

interface Scopes {
    val auth: ScopeHolder
}

expect object MyKrosstalk : Krosstalk<KotlinxSerializers>, Scopes

expect suspend fun doThing(data: Data): List<String>
expect suspend fun doAuthThing(num: Int): Data