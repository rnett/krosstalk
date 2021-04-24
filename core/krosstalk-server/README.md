# Module Krosstalk Server

Krosstalk APIs necessary for defining a server.

To define a Krosstalk server, create an object that inherits from `Krosstalk` and `KrosstalkServer<S>`, where `S`
is the scope type of the server plugin you are using. Server plugins will usually provide an interface to do this that
also sets the `server` type, i.e. `KtorKrosstalkServer`. You will then set the `server` property to an instance of your
server plugin's handler, i.e. `KtorKrosstalkServer`.

However, there is usually an extra step. Servers usually have their own entrypoints, and we probably wouldn't want to
start a new server just for our Krosstalk. Because of this, the server handlers provided by plugins are usually objects
that don't do anything. The server plugin should provide some way of adding the Krosstalk's methods to the server, for
example the Ktor server's [defineKtor].

A minimal example with authentication using the Ktor server looks like this:

```kotlin
actual object MyKrosstalk : Krosstalk(), KrosstalkServer<KtorServerScope<*>> {
    actual override val serialization = KotlinxBinarySerializationHandler(Cbor { })
    override val server = KtorServer

    actual object Auth : Scope, KtorServerBasicAuth<User>("auth", {
        validate {
            if (validUsers[it.name] == it.password)
                User(it.name)
            else
                null
        }
    })
}

fun main() {
    embeddedServer(CIO, 8080, "localhost") {
        MyKrosstalk.defineKtor(this)
    }.start(true)
}
```

Note that the actual scope objects need to explicitly inherit from `Scope`. This is due
to [KT-20641](https://youtrack.jetbrains.com/issue/KT-20641).

A standalone server would look the same, except without the `actual`s.

For more examples, see `tests`.

Plugin APIs are in `com.rnett.krosstalk.server.plugin`.

# Package com.rnett.krosstalk.server

Krosstalk APIs necessary for defining a server. See the module description.

# Package com.rnett.krosstalk.server.plugin

APIs for defining server plugins. For an example, see the Krosstalk Ktor server.

Server plugins will need to define a scope for their implementation that implements `ServerScope`, and a server handler
that implements `ServerHandler<S>` where `S` is their scope type. Their server handler can then be used
in `KrosstalkServer`s.

The scope interface (or abstract class, but it's usually an interface) will need to define the methods that the server
handler will call to extract the scope's data from a request. Subclasses can then implement these methods. Since users
define scopes by defining objects, all scope classes should be `open`, and `abstract` methods should be preferred over
lambda properties or parameters.

Because of the reason outlined in the module description (servers having their own entrypoints), `ServerHandler` has no
methods. Instead, plugins should provide easy to use methods to register a Krosstalk's methods with their server
implementation. For example, see the Ktor server's [defineKtor].