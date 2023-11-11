package com.rnett.krosstalk.serialization

import com.rnett.krosstalk.metadata.KrosstalkMethod
import com.rnett.krosstalk.metadata.KrosstalkSpec
import kotlinx.serialization.*
import kotlinx.serialization.modules.SerializersModule

internal sealed interface KotlinxKrosstalkFormat {
    fun <T> decodeFromByteArray(serializer: DeserializationStrategy<T>, data: ByteArray): T
    fun <T> encodeToByteArray(serializer: SerializationStrategy<T>, data: T): ByteArray

    val serializersModule: SerializersModule

    class Binary(private val format: BinaryFormat) : KotlinxKrosstalkFormat {
        override fun <T> decodeFromByteArray(serializer: DeserializationStrategy<T>, data: ByteArray): T =
            format.decodeFromByteArray(serializer, data)

        override fun <T> encodeToByteArray(serializer: SerializationStrategy<T>, data: T): ByteArray =
            format.encodeToByteArray(serializer, data)

        override val serializersModule: SerializersModule = format.serializersModule
    }

    class TextUTF8(private val format: StringFormat) : KotlinxKrosstalkFormat {
        override fun <T> decodeFromByteArray(serializer: DeserializationStrategy<T>, data: ByteArray): T =
            format.decodeFromString(serializer, data.decodeToString())

        override fun <T> encodeToByteArray(serializer: SerializationStrategy<T>, data: T): ByteArray =
            format.encodeToString(serializer, data).encodeToByteArray()

        override val serializersModule: SerializersModule = format.serializersModule
    }
}

@OptIn(ExperimentalSerializationApi::class)
internal sealed class KotlinxKrosstalkSerialization(protected val format: KotlinxKrosstalkFormat) :
    KrosstalkSerialization {

    private val argSerializers = mutableMapOf<String, StructSerializer>()

    internal fun argumentSerializer(method: KrosstalkMethod) = argSerializers.getValue(method.methodFullyQualifiedName)

    override fun initializeForSpec(spec: KrosstalkSpec<*>) {
        spec.methods.values.forEach {
            argSerializers[it.methodFullyQualifiedName] =
                StructSerializer(it.parameters.mapValues { it.value.type }, format.serializersModule)
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
internal class KotlinxServerSerialization(format: KotlinxKrosstalkFormat) :
    KotlinxKrosstalkSerialization(format), KrosstalkServerSerialization {

    override fun deserializeArguments(method: KrosstalkMethod, data: ByteArray): Map<String, Any?> {
        return format.decodeFromByteArray(argumentSerializer(method), data)
    }

    override fun serializeReturnValue(method: KrosstalkMethod, data: Any?): ByteArray {
        return format.encodeToByteArray(format.serializersModule.serializer(method.returnType), data)
    }
}

@OptIn(ExperimentalSerializationApi::class)
internal class KotlinxClientSerialization(format: KotlinxKrosstalkFormat) : KotlinxKrosstalkSerialization(format),
    KrosstalkClientSerialization {
    override fun serializeArguments(method: KrosstalkMethod, argumentValues: Map<String, Any?>): ByteArray {
        return format.encodeToByteArray(argumentSerializer(method), argumentValues)
    }

    override fun deserializeReturnValue(method: KrosstalkMethod, data: ByteArray): Any? {
        "".encodeToByteArray()
        return format.decodeFromByteArray(format.serializersModule.serializer(method.returnType), data)
    }
}

public fun KrosstalkClientSerialization(format: BinaryFormat): KrosstalkClientSerialization =
    KotlinxClientSerialization(KotlinxKrosstalkFormat.Binary(format))

public fun KrosstalkClientSerialization(format: StringFormat): KrosstalkClientSerialization =
    KotlinxClientSerialization(KotlinxKrosstalkFormat.TextUTF8(format))

public fun KrosstalkServerSerialization(format: BinaryFormat): KrosstalkServerSerialization =
    KotlinxServerSerialization(KotlinxKrosstalkFormat.Binary(format))

public fun KrosstalkServerSerialization(format: StringFormat): KrosstalkServerSerialization =
    KotlinxServerSerialization(KotlinxKrosstalkFormat.TextUTF8(format))
