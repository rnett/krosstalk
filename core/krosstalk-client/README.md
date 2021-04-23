# Module Krosstalk Client

Krosstalk APIs necessary for defining a client.

<br></br>

To define a Krosstalk client, create an object that inherits from `Krosstalk` and `KrosstalkClient<S>`, where `S`
is the scope type of the client plugin you are using. Client plugins will usually provide an interface to do this that
also sets the `client` type, i.e. `KtorKrosstalkClient`. You will then set the `client` property to an instance of your
client plugin's handler, i.e. `KtorClient` and the `serverUrl` property to the target server's URL.

<br></br>

A minimal example using the Ktor client looks like this:

```kotlin
actual object MyKrosstalk : Krosstalk(), KtorKrosstalkClient {
    actual override val serialization = KotlinxBinarySerializationHandler(Cbor { })

    override val client: KtorClient = KtorClient(HttpClient(Apache))
    override val serverUrl: String = "http://localhost:8080"

    actual object Auth : Scope, KtorClientBasicAuth()
}
```

A standalone client would look the same, except without the `actual`s.

<br></br>

For more examples, see `tests`.

<br></br>

Plugin APIs are in `com.rnett.krosstalk.client.plugin`.

# Package com.rnett.krosstalk.client

Krosstalk APIs necessary for defining a client.

# Package com.rnett.krosstalk.client.plugin

APIs for defining client plugins. For an example, see the Krosstalk Ktor client.

<br></br>

Client plugins will need to define a scope for their implementation that implements `ClientScope`, and a client handler
that implements `ClientHandler<S>` where `S` is their scope type. Their client handler can then be used
in `KrosstalkClient`s.

<br></br>

The scope interface (or abstract class, but it's uslally an interface) will need to define the methods that the client
handler will call to apply the scope to a request. Subclasses can then implement these methods. Since users define
scopes by defining objects, all scope classes should be `open`, and `abstract` methods should be preferred over lambda
properties or parameters.

<br></br>

The client handler only has one method to implement: `ClientHandler.sendKrosstalkRequest`, which sends a http request
and returns the response (as an `InternalKrosstalkResponse`). This method must apply all of the method's scopes, which
will be passed as `AppliedScope<S, *>`
where `S` is the type passed to it's `ClientHandler`, which should be your plugin's scope type.