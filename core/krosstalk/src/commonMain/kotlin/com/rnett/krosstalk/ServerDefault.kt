package com.rnett.krosstalk

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(InternalKrosstalkApi::class)
class UnrealizedServerDefault @InternalKrosstalkApi constructor() : KrosstalkException("ServerDefault was not realized")

class ServerDefault<out T> @PublishedApi internal constructor(@PublishedApi internal val _value: Any?) {

    @OptIn(InternalKrosstalkApi::class)
    inline val value
        get(): T {
            if (isNone())
                throw UnrealizedServerDefault()
            return _value as T
        }

    @PublishedApi
    internal object None {

    }

    @InternalKrosstalkApi
    fun isNone() = _value == None
}

@PublishedApi
internal fun noneServerDefault(): ServerDefault<Nothing> = ServerDefault(ServerDefault.None)

inline fun <T> ServerDefault(value: () -> T): ServerDefault<T> {
    contract { callsInPlace(value, InvocationKind.EXACTLY_ONCE) }
    return ServerDefault(value())
}