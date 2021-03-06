package com.rnett.krosstalk.client_test

import kotlinx.serialization.Serializable

@Serializable
data class ItemSetRequest(val id: Int, val item: Item)

@Serializable
data class Item(val id: Int, val name: String)