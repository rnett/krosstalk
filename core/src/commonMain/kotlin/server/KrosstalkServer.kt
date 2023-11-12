package com.rnett.krosstalk.server

import com.rnett.krosstalk.client.RequestMaker
import com.rnett.krosstalk.error.ExceptionEncoding
import com.rnett.krosstalk.error.KrosstalkResult
import com.rnett.krosstalk.error.KrosstalkResultStatus
import com.rnett.krosstalk.metadata.KrosstalkSpec
import com.rnett.krosstalk.serialization.KrosstalkServerSerialization
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

public abstract class KrosstalkServer<T>(
    private val serialization: KrosstalkServerSerialization,
    @Suppress("MemberVisibilityCanBePrivate") protected val spec: KrosstalkSpec<T>
) {

    internal val theSpec get() = spec

    init {
        serialization.initializeForSpec(spec)
    }

    internal suspend fun receive(methodName: String, data: ByteArray): KrosstalkResult {
        return try {
            val method = spec.method(methodName)
            val arguments = serialization.deserializeArguments(method, data)
            val result = runCatching { invoke(methodName, arguments) }
                .map { KrosstalkResult(serialization.serializeReturnValue(method, it), KrosstalkResultStatus.SUCCESS) }
                .getOrElse { KrosstalkResult(encodeUserException(it), KrosstalkResultStatus.USER_ERROR) }
            result
        } catch (t: Throwable) {
            KrosstalkResult(encodeKrosstalkException(t), KrosstalkResultStatus.KROSSTALK_ERROR)
        }
    }

    private fun encodeUserException(throwable: Throwable): ByteArray {
        return ExceptionEncoding.encode(KrosstalkResultStatus.USER_ERROR, throwable, shouldExposeException(throwable))
    }

    private fun encodeKrosstalkException(throwable: Throwable): ByteArray {
        return ExceptionEncoding.encode(
            KrosstalkResultStatus.KROSSTALK_ERROR,
            throwable,
            shouldExposeException(throwable)
        )
    }

    /**
     * Whether [throwable]'s type and message should be exposed to users if occurs during a request
     */
    protected open fun shouldExposeException(throwable: Throwable): Boolean {
        return true
    }

    protected abstract suspend fun invoke(methodName: String, arguments: Map<String, Any?>): Any?
}

/**
 * Mount request handlers for a server.
 * The handlers should generally accept the headers [RequestMaker] makes, and respond with `application/octet-stream` stream content.
 */
public fun KrosstalkServer<*>.mount(mounter: (subPath: String, invoke: suspend (ByteArray) -> KrosstalkResult) -> Unit) {
    contract {
        callsInPlace(mounter, InvocationKind.AT_LEAST_ONCE)
    }
    theSpec.methods.keys.forEach { method ->
        mounter.invoke(method) {
            this.receive(method, it)
        }
    }
}

/**
 * Mount request handlers for a server.
 * The handlers should generally accept the headers [RequestMaker] makes, and respond with `application/octet-stream` stream content.
 */
public fun <T> KrosstalkSpec<T>.mount(mounter: (subPath: String, invoke: suspend (KrosstalkServer<T>, ByteArray) -> KrosstalkResult) -> Unit) {
    contract {
        callsInPlace(mounter, InvocationKind.AT_LEAST_ONCE)
    }
    methods.keys.forEach { method ->
        mounter.invoke(method) { server, data ->
            server.receive(method, data)
        }
    }
}