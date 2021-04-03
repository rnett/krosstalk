package com.rnett.krosstalk

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(InternalKrosstalkApi::class)
class UnrealizedServerDefaultException @InternalKrosstalkApi constructor() : KrosstalkException("ServerDefault was not realized")

class ServerDefault<out T> @PublishedApi internal constructor(@PublishedApi internal val _value: Any?) {

    @OptIn(InternalKrosstalkApi::class)
    inline val value
        get(): T {
            if (isNone())
                throw UnrealizedServerDefaultException()
            return _value as T
        }

    @PublishedApi
    internal object None {

    }
}

@InternalKrosstalkApi
@Deprecated("Internal use only, accessible ServerDefaults will never be None")
fun ServerDefault<*>.isNone() = _value == ServerDefault.None

@PublishedApi
internal fun noneServerDefault(): ServerDefault<Nothing> = ServerDefault(ServerDefault.None)

inline fun <T> ServerDefault(value: () -> T): ServerDefault<T> {
    contract { callsInPlace(value, InvocationKind.EXACTLY_ONCE) }
    return ServerDefault(value())
}