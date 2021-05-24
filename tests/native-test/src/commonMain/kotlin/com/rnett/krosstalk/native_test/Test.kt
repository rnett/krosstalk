package com.rnett.krosstalk.native_test

import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.Scope
import com.rnett.krosstalk.annotations.KrosstalkMethod
import com.rnett.krosstalk.serialization.KotlinxBinarySerializationHandler
import kotlinx.serialization.Serializable

@Serializable
data class Item(val id: Int, val name: String)


expect object MyKrosstalk : Krosstalk {
    override val serialization: KotlinxBinarySerializationHandler

    object Auth : Scope
}

@KrosstalkMethod(MyKrosstalk::class)
expect suspend fun testBasic(id: Int, name: String): Item