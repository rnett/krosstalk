package com.rnett.krosstalk.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.plus
import org.junit.jupiter.api.Test
import kotlin.reflect.typeOf
import kotlin.test.assertEquals

@OptIn(ExperimentalSerializationApi::class)
class StructSerializerTest {

    @Test
    fun `serialized basic struct`() {
        val def = mapOf(
            "a" to typeOf<Int>(),
            "b" to typeOf<String>()
        )

        val data = mapOf("a" to 2, "b" to "test")

        val json = Json

        val serializer = StructSerializer(def, json.serializersModule)

        val serialized = json.encodeToString(serializer, data)

        assertEquals("""{"a":2,"b":"test"}""", serialized)

        val deserialized = json.decodeFromString(serializer, serialized)

        assertEquals(data, deserialized)
    }

    @Test
    fun `serialized complex struct`() {
        val def = mapOf(
            "a" to typeOf<List<Int>>(),
            "b" to typeOf<Map<Int, String>>(),
            "another" to typeOf<Pair<String, String>>()
        )

        val data = mapOf("a" to listOf(2, 3), "b" to mapOf(1 to "1", 2 to "2"), "another" to ("a" to "test"))

        val json = Json

        val serializer = StructSerializer(def, json.serializersModule)

        val serialized = json.encodeToString(serializer, data)

        assertEquals("""{"a":[2,3],"b":{"1":"1","2":"2"},"another":{"first":"a","second":"test"}}""", serialized)

        val deserialized = json.decodeFromString(serializer, serialized)

        assertEquals(data, deserialized)
    }

    data class TestData(val a: Int, val b: Int)

    object TestDataSerializer : KSerializer<TestData> {
        override fun deserialize(decoder: Decoder): TestData {
            return decoder.decodeStructure(descriptor) {
                var a = -1
                var b = -1
                while (true) {
                    when (val index = decodeElementIndex(descriptor)) {
                        0 -> a = decodeIntElement(descriptor, 0)
                        1 -> b = decodeIntElement(descriptor, 1)
                        CompositeDecoder.DECODE_DONE -> break
                        else -> error("Unexpected index: $index")
                    }
                }
                TestData(a, b)
            }
        }

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("TestData") {
            element<Int>("a")
            element<Int>("b")
        }

        override fun serialize(encoder: Encoder, value: TestData) {
            encoder.encodeStructure(descriptor) {
                encodeIntElement(descriptor, 0, value.a)
                encodeIntElement(descriptor, 1, value.b)
            }
        }
    }

    @Test
    fun `uses contextual serializers`() {
        val def = mapOf(
            "t" to typeOf<TestData>()
        )

        val data = mapOf("t" to TestData(1, 2))

        val json = Json {
            serializersModule += SerializersModule {
                contextual(TestDataSerializer)
            }
        }

        val serializer = StructSerializer(def, json.serializersModule)

        val serialized = json.encodeToString(serializer, data)

        assertEquals("""{"t":{"a":1,"b":2}}""", serialized)

        val deserialized = json.decodeFromString(serializer, serialized)

        assertEquals(data, deserialized)
    }
}