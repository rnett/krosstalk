package com.rnett.krosstalk

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(InternalKrosstalkApi::class)
public class UnrealizedServerDefaultException @InternalKrosstalkApi constructor() :
    KrosstalkException("ServerDefault was not realized on the server side, this should be impossible.")

public class ServerDefault<out T> @PublishedApi internal constructor(@PublishedApi internal val _value: Any?) {

    @OptIn(InternalKrosstalkApi::class)
    public inline val value: T
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
public fun ServerDefault<*>.isNone(): Boolean = _value == ServerDefault.None

@PublishedApi
internal fun noneServerDefault(): ServerDefault<Nothing> = ServerDefault(ServerDefault.None)

public inline fun <T> ServerDefault(value: () -> T): ServerDefault<T> {
    contract { callsInPlace(value, InvocationKind.EXACTLY_ONCE) }
    return ServerDefault(value())
}