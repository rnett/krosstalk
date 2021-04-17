package com.rnett.krosstalk.compose_test

import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object InstantSerializer : KSerializer<Instant> {
    override fun deserialize(decoder: Decoder): Instant {
        return Instant.fromEpochMilliseconds(decoder.decodeLong())
    }

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlinx.datetime.Instant", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeLong(value.toEpochMilliseconds())
    }

}

@Serializable
data class ToDo(val date: @Serializable(InstantSerializer::class) Instant, val message: String) {}