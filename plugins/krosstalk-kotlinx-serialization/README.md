# Module Krosstalk Kotlinx-serialization

A Krosstalk serialization plugin using [Kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization).

Artifact: `com.github.rnett.krosstalk:krosstalk-kotlinx-serialization`

Provides the JSON format by default, others can be used as well.

[KotlinxBinarySerializationHandler] is used for binary formats, and [KotlinxStringSerializationHandler] for string
formats.

Arguments are serialized as if they were properties in a class.

Annotations on parameters are not propagated to serialization ([KT-29919](https://youtrack.jetbrains.com/issue/KT-29919)),
but serializers are gotten from the handler's format's `serializersModule` so there is no need for `@Contextual`.