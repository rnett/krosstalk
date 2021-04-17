package com.rnett.krosstalk.compose_test

import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.KrosstalkResult
import com.rnett.krosstalk.Scope
import com.rnett.krosstalk.ScopeInstance
import com.rnett.krosstalk.annotations.ExplicitResult
import com.rnett.krosstalk.annotations.KrosstalkMethod
import com.rnett.krosstalk.serialization.KotlinxBinarySerializationHandler

expect object TodoKrosstalk : Krosstalk {
    override val serialization: KotlinxBinarySerializationHandler

    object Auth : Scope
}

@KrosstalkMethod(TodoKrosstalk::class)
@ExplicitResult
expect suspend fun tryLogin(auth: ScopeInstance<TodoKrosstalk.Auth>): KrosstalkResult<Unit>

@KrosstalkMethod(TodoKrosstalk::class)
expect suspend fun getTodos(auth: ScopeInstance<TodoKrosstalk.Auth>): List<ToDo>

@KrosstalkMethod(TodoKrosstalk::class)
expect suspend fun addTodo(message: String, auth: ScopeInstance<TodoKrosstalk.Auth>): List<ToDo>

@KrosstalkMethod(TodoKrosstalk::class)
expect suspend fun removeTodo(idx: Int, auth: ScopeInstance<TodoKrosstalk.Auth>): List<ToDo>