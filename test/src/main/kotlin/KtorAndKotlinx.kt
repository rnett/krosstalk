package com.rnett.krosstalk.test

import com.rnett.krosstalk.Krosstalk
import kotlinx.serialization.Serializable

@Serializable
data class TestRequest(val data: List<String>, val i: Int)

@Serializable
data class TestResponse(val lengthPlusI: Int, val concat: String)

@Krosstalk
interface KtorAndKotlinx {

    suspend fun testEndpoint(request: TestRequest): TestResponse

    companion object
}