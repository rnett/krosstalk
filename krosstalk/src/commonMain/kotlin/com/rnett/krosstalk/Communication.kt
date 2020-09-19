package com.rnett.krosstalk

import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor

interface ClientHandler<C : ClientScope> {
    suspend fun sendKrosstalkRequest(methodName: String, body: ByteArray, scopes: List<C>): ByteArray
}

interface ServerHandler<S : ServerScope>

sealed class Optional {
    object None : Optional()
    data class Some(val value: Any?) : Optional()

    val isSome inline get() = this is Some

    companion object {
        operator fun invoke(value: Any?) = Some(value)
    }
}

@Serializable
data class KrosstalkCall(val function: String,
                         val parameters: Map<String, ByteArray>,
                         val instanceReceiver: ByteArray?,
                         val extensionReceiver: ByteArray?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KrosstalkCall) return false

        if (function != other.function) return false
        if (parameters != other.parameters) return false
        if (instanceReceiver != null) {
            if (other.instanceReceiver == null) return false
            if (!instanceReceiver.contentEquals(other.instanceReceiver)) return false
        } else if (other.instanceReceiver != null) return false
        if (extensionReceiver != null) {
            if (other.extensionReceiver == null) return false
            if (!extensionReceiver.contentEquals(other.extensionReceiver)) return false
        } else if (other.extensionReceiver != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = function.hashCode()
        result = 31 * result + parameters.hashCode()
        result = 31 * result + (instanceReceiver?.contentHashCode() ?: 0)
        result = 31 * result + (extensionReceiver?.contentHashCode() ?: 0)
        return result
    }
}


suspend inline fun <T, K, reified C : ClientScope> K.call(methodName: String,
                                                          parameters: Map<String, *>,
                                                          extensionReceiver: Optional = Optional.None,
                                                          instanceReceiver: Optional = Optional.None
): T where K : KrosstalkClient<C>, K : Krosstalk {
    val method = methods[methodName] ?: error("Unknown method $methodName")
    val serializedParams = parameters.mapValues {
        val serializer = method.serializers.paramSerializers[it.key]
                ?: error("No serializer found for param ${it.key}")
        (serializer as Serializer<Any?>).serialize(it.value)
    }

    val instance = if (instanceReceiver is Optional.Some) {
        ((method.serializers.instanceReceiverSerializer
                ?: error("No instance receiver serializer found, but argument passed"))
                as Serializer<Any?>).serialize(instanceReceiver.value)
    } else null

    val extension = if (extensionReceiver is Optional.Some) {
        ((method.serializers.extensionReceiverSerializer
                ?: error("No extension receiver serializer found, but argument passed"))
                as Serializer<Any?>).serialize(extensionReceiver.value)
    } else null

    val data = Cbor.encodeToByteArray(
        KrosstalkCall.serializer(),
        KrosstalkCall(methodName, serializedParams, instance, extension)
    )
    val result = client.sendKrosstalkRequest(methodName, data, activeScopes.values.map {
        it as? C ?: error("Scope $it was not of required type ${C::class}")
    })
    return method.serializers.resultSerializer.deserialize(result) as T
}