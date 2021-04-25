# Module Krosstalk Kotlinx Serialization

A Krosstalk serialization plugin using [Kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization).

Provides the JSON format by default, others can be used as well.

[KotlinxBinarySerializationHandler] is used for binary formats, and [KotlinxStringSerializationHandler] for string
formats.

`KotlinxJsonObjectSerializationHandler` is a special, JSON-only serialization handler that when serializing multiple
arguments, wraps them in a JSON object instead of a Kotlin Map.  **This is almost always required when interacting with
a non-Krosstalk API.**