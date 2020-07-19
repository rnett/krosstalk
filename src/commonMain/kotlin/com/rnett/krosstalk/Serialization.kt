package com.rnett.krosstalk

import kotlinx.serialization.KSerializer
import kotlinx.serialization.cbor.Cbor
import kotlin.reflect.KCallable

interface SerializationHandler<D>{
    fun <T> getSerializers(method: KCallable<T>, extraData: D): MethodSerializers<T>
}

interface Serializer<T>{
    fun deserialize(data: ByteArray): T
    fun serialize(data: T): ByteArray
}

class KotlinxSerializer<T>(val serializer: KSerializer<T>): Serializer<T>{
    override fun deserialize(data: ByteArray): T {
        return Cbor.load(serializer, data);
    }

    override fun serialize(data: T): ByteArray {
        return Cbor.dump(serializer, data);
    }

}

data class KotlinxSerializers(val paramSerializers: Map<String, KSerializer<*>>, val returnSerializer: KSerializer<*>)

object KotlinxSerializationHandler: SerializationHandler<KotlinxSerializers>{
    override fun <T> getSerializers(method: KCallable<T>, extraData: KotlinxSerializers): MethodSerializers<T> {
        return MethodSerializers(extraData.paramSerializers.mapValues { KotlinxSerializer(it.value) }, KotlinxSerializer(extraData.returnSerializer) as KotlinxSerializer<T>)
    }
}