# Module Krosstalk

Common Krosstalk APIs that are used be both clients and servers, and everything required to define an `expect`
Krosstalk. Includes `Krosstalk`, `Scope`, `KrosstalkResult`, etc.

Usually the only source set that needs to explicitly depend on this artifact is the `common` one, source sets or modules
with Krosstalk client or servers will inherit it.

The reason this artifact is separate is to allow for defining a common interface for the client and server.
Kotlin's `expect-actual` mechanism can be used to force client and server methods to have the same signature, and
configuration annotations can be placed on the `expect` methods.

To define a Krosstalk object, create an object that inherits from `Krosstalk`. This object acts as a regristry for
Krosstalk RPC methods you define, and provides a common point to configure things like the serialization handler, and
the url prefix. Krosstalk client and server objects additionally inherit from `KrosstalkClient` or `KrosstalkServer`,
respectively, which provide additional configuration options.

A minimal example of an `expect` Krosstalk object looks like this:

```kotlin
expect object MyKrosstalk : Krosstalk {
    override val serialization: KotlinxBinarySerializationHandler

    object Auth : Scope
}
```

Methods can then be defined like:

```kotlin
@KrosstalkMethod(MyKrosstalk::class)
expect suspend fun basicTest(data: Data): List<String>
```

If you are using an `expect` Krosstalk, all configuration annotations should be used on the `expect` method, not
the `actual`s. Configuring the `actuals` will result in a compiler error.

For configuration options, see the annotations in [com.rnett.annotations].

# Package com.rnett.krosstalk

The main package. See the module docs.

# Package com.rnett.krosstalk.annotations

Annotations that can be used to configure Krosstalk methods.

# Package com.rnett.krosstalk.serialization

Serialization APIs.

# Package com.rnett.krosstalk.serialization.plugin

APIs necessary to define serialization plugins. Look at the Kotlinx serialization plugin for examples.

# Package com.rnett.krosstalk.client

APIs that need to be accessible to common code, but are only used by clients.

# Package com.rnett.krosstalk.server

APIs that need to be accessible to common code, but are only used by servers.