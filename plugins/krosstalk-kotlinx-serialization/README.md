# Module Krosstalk Kotlinx-serialization

A Krosstalk serialization plugin using [Kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization).

Artifact: `com.github.rnett.krosstalk:krosstalk-kotlinx-serialization`

Provides the JSON format by default, others can be used as well.

[KotlinxBinarySerializationHandler] is used for binary formats, and [KotlinxStringSerializationHandler] for string
formats.

Arguments are serialized as if they were properties in a class.