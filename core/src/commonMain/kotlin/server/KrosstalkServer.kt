package com.rnett.krosstalk.server

import com.rnett.krosstalk.metadata.KrosstalkSpec
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

public abstract class KrosstalkServer<T>(
    private val serialization: KrosstalkServerSerialization,
    protected val spec: KrosstalkSpec<T>
) {

    internal val theSpec get() = spec

    init {
        serialization.initializeForSpec(spec)
    }

    internal suspend fun receive(methodName: String, data: ByteArray): ByteArray {
        val method = spec.method(methodName)
        val arguments = serialization.deserializeArguments(method, data)
        val result = invoke(methodName, arguments)
        return serialization.serializeReturnValue(method, result)
    }

    protected abstract suspend fun invoke(methodName: String, arguments: Map<String, Any?>): Any?
}

public fun KrosstalkServer<*>.mount(mounter: (subPath: String, invoke: suspend (ByteArray) -> ByteArray) -> Unit) {
    contract {
        callsInPlace(mounter, InvocationKind.AT_LEAST_ONCE)
    }
    theSpec.methods.keys.forEach { method ->
        mounter.invoke(method) {
            this.receive(method, it)
        }
    }
}

public fun <T> KrosstalkSpec<T>.mount(mounter: (subPath: String, invoke: suspend (KrosstalkServer<T>, ByteArray) -> ByteArray) -> Unit) {
    contract {
        callsInPlace(mounter, InvocationKind.AT_LEAST_ONCE)
    }
    methods.keys.forEach { method ->
        mounter.invoke(method) { server, data ->
            server.receive(method, data)
        }
    }
}