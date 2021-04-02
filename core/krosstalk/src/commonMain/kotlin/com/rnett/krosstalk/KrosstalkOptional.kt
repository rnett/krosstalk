package com.rnett.krosstalk

import kotlin.contracts.contract

//TODO what to do with nullable arguments
//TODO forbid @Optional KrosstalkOptional?
//TODO support as return type?
//TODO use in KrosstalkResult API?
sealed class KrosstalkOptional<out T> {
    data class Some<out T>(val value: T) : KrosstalkOptional<T>()
    object None : KrosstalkOptional<Nothing>()

    fun isSome(): Boolean {
        contract {
            returns(true) implies (this@KrosstalkOptional is Some)
            returns(false) implies (this@KrosstalkOptional is None)
        }
        return this is Some
    }

    fun isNone(): Boolean {
        contract {
            returns(true) implies (this@KrosstalkOptional is None)
            returns(false) implies (this@KrosstalkOptional is Some)
        }
        return this is None
    }

    fun assertSome() {
        contract { returns() implies (this@KrosstalkOptional is Some) }
        if (this is None)
            error("Requires KrosstalkOptional to be Some, but was None.")
    }

    inline val valueOrNull get() = if (this is Some) value else null

    inline val valueOrThrow: T
        get() {
            assertSome()
            return valueOrNull!!
        }

    companion object {
        inline operator fun <T> invoke(value: T) = KrosstalkOptional.Some(value)
        inline fun <T> ifNotNull(value: T?) = if (value == null) None else Some(value)
    }
}

inline fun <T> T?.noneIfNull() = KrosstalkOptional.ifNotNull(this)

inline infix fun <R, T : R> KrosstalkOptional<T>.orElse(value: () -> R): R = if (this is KrosstalkOptional.Some) this.value else value()
inline operator fun <R, T : R> KrosstalkOptional<T>.invoke(value: () -> R) = orElse(value)

inline infix fun <R, T : R> KrosstalkOptional<T>.orDefault(value: R): R = if (this is KrosstalkOptional.Some) this.value else value
inline operator fun <R, T : R> KrosstalkOptional<T>.invoke(value: R) = orDefault(value)