# Writing Krosstalk Plugins

Plugin definition APIs are generally marked with `@KrosstalkPluginApi` and defined in `*.plugin` packages. The
`KrosstalkPluginApi` annotation should **not** be propagated to users of your plugin. Serialization, client, and server
plugins should depend on the core, client, and server artifacts, respectively, and expose those dependencies (i.e.
use `api()`).

**If you write a plugin, please strongly consider contributing it, either via a PR or in your own repo.**  Even if it's
not polished, it's very helpful for others writing their own using the same backing implementation. I'm happy to help
with plugin API issues, or polishing up contributed plugins in a PR. If you want to keep the plugin in your own repo,
let me know, and I'll add a link to it to the README.

### Contents

* [Serialization](#serialization)
* [Client](#client)
* [Server](#server)

## Serialization

For an example, see [krosstalk-kotlinx-serialization](./plugins/krosstalk-kotlinx-serialization). This guide will follow
its implementation.

A serialization plugin needs to define a `SerializationHandler`, which is usable in `Krosstalk` objects. To define a
serialization handler, you will first have to define a serializer implementation, implementing
`Serializer<T, S>` (
see [Serializers.kt](./core/krosstalk/src/commonMain/kotlin/com/rnett/krosstalk/serialization/plugin/Serializers.kt))
. A serializer will serialize objects of type `T` to format `S`, and deserialize from `S` to `T`.
`S` is almost always `ByteArray` or `String`, and as such typealiases `StringSerializer<T>` and
`BinarySerializer<T>` are provided.

For Kotlinx serialization, this looks
like ([source](./plugins/krosstalk-kotlinx-serialization/src/commonMain/kotlin/com/rnett/krosstalk/serialization/KotlinxSerializers.kt#L18)):

```kotlin
@OptIn(KrosstalkPluginApi::class)
public data class KotlinxBinarySerializer<T>(val serializer: KSerializer<T>, val format: BinaryFormat) :
    BinarySerializer<T> {
    override fun deserialize(data: ByteArray): T = format.decodeFromByteArray(serializer, data)

    override fun serialize(data: T): ByteArray = format.encodeToByteArray(serializer, data)
}

@OptIn(KrosstalkPluginApi::class)
public data class KotlinxStringSerializer<T>(val serializer: KSerializer<T>, val format: StringFormat) :
    StringSerializer<T> {
    override fun deserialize(data: String): T = format.decodeFromString(serializer, data)

    override fun serialize(data: T): String = format.encodeToString(serializer, data)
}
```

Note the use of `@OptIn(KrosstalkPluginApi::class)`: we don't want to propagate `KrosstalkPluginApi` to users of our
plugin.

Next, you need to define the `SerializationHandler<S>`, where `S` is again the format (see
[Serializers.kt](./core/krosstalk/src/commonMain/kotlin/com/rnett/krosstalk/serialization/plugin/SerializationHandlers.kt))
. You need to define `getSerializer` to get a serializer (presumably of the type you defined) for a `KType`, methods to
serialize and deserialize sets of arguments given the arguments and their serializers, and
a `transformer: SerializedFormatTransformer<S>`, which can transform your serialized format (`S`) to a `String` and
`ByteArray`.  `contentType` can optionally be overridden to set the content type used by requests and responses.

Transformers for `String` and `ByteArray` are already defined (`StringTransformer` and `ByteTransformer`, respectively),
and you can define custom ones (see
[SerializationFormats.kt](./core/krosstalk/src/commonMain/kotlin/com/rnett/krosstalk/serialization/plugin/SerialziationFormats.kt))
.

Some helper case classes are provided for defining serialization handlers: `BaseSerializationHandler` and
`ArgumentSerializationHandler`.  `BaseSerializationHandler` only defines `transformer` as a constructor property,
but `ArgumentSerializationHandler` goes a bit further and handles argument map serialization for you, only requiring you
to implement `Map<String, S>` serialization and deserialization methods (**note that `S` is your serialized data
format**).

Examples of using `ArgumentSerializationHandler` can be seen in `KotlinxStringSerializationHandler` and
`KotlinxBinarySerializationHandler`
([source](./plugins/krosstalk-kotlinx-serialization/src/commonMain/kotlin/com/rnett/krosstalk/serialization/KotlinxSerializers.kt#L41)):

```kotlin
public data class KotlinxBinarySerializationHandler(val format: BinaryFormat) :
    ArgumentSerializationHandler<ByteArray>(ByteTransformer) {
    override fun serializeArguments(serializedArguments: Map<String, ByteArray>): ByteArray {
        return format.encodeToByteArray(mapSerializer, serializedArguments)
    }

    override fun deserializeArguments(arguments: ByteArray): Map<String, ByteArray> {
        return format.decodeFromByteArray(mapSerializer, arguments)
    }

    override fun getSerializer(type: KType): KotlinxBinarySerializer<*> =
        KotlinxBinarySerializer(serializer(type), format)

    private val mapSerializer = serializer<Map<String, ByteArray>>()
    override val contentType: String = byteArrayContentType
}

public data class KotlinxStringSerializationHandler(val format: StringFormat) :
    ArgumentSerializationHandler<String>(StringTransformer) {
    override fun serializeArguments(serializedArguments: Map<String, String>): String {
        return format.encodeToString(mapSerializer, serializedArguments)
    }

    override fun deserializeArguments(arguments: String): Map<String, String> {
        return format.decodeFromString(mapSerializer, arguments)
    }

    override fun getSerializer(type: KType): KotlinxStringSerializer<*> =
        KotlinxStringSerializer(serializer(type), format)

    private val mapSerializer = serializer<Map<String, String>>()
    override val contentType: String = stringContentType
}
```

However, `ArgumentSerializationHandler` is not always suitable. For example, when using Json, it is standard to
serialize a map of arguments as an object with each argument as a field. However, with
`ArgumentSerializationHandler`, we serialize each argument separately, before merging them into a map, so everything
would be unnecessarily wrapped in strings. In situations where you are using a non-Krosstalk client or server, this will
usually cause issues, so you can extend `BaseSerializationHandler` instead of `ArgumentSerializationHandler`
and define the combination step yourself. An example of this is `KotlinxJsonObjectSerializationHandler`
([source](https://github.com/rnett/krosstalk/blob/main/plugins/krosstalk-kotlinx-serialization/src/commonMain/kotlin/com/rnett/krosstalk/serialization/KotlinxSerializers.kt#L67)):

```kotlin
public data class KotlinxJsonObjectSerializationHandler(val format: Json) :
    BaseSerializationHandler<String>(StringTransformer) {
    override fun getSerializer(type: KType): KotlinxStringSerializer<*> =
        KotlinxStringSerializer(serializer(type), format)

    override fun serializeArguments(arguments: Map<String, *>, serializers: ArgumentSerializers<String>): String {
        val jsonObject = buildJsonObject {
            arguments.forEach { (key, data) ->
                put(
                    key,
                    format.encodeToJsonElement((serializers[key] as KotlinxStringSerializer<Any?>).serializer, data)
                )
            }
        }
        return jsonObject.toString()
    }

    override fun deserializeArguments(arguments: String, serializers: ArgumentSerializers<String>): Map<String, *> {
        val jsonObject = format.parseToJsonElement(arguments).jsonObject
        return jsonObject.mapValues { (key, data) ->
            Json.decodeFromJsonElement((serializers[key] as KotlinxStringSerializer<Any?>).serializer, data)
        }
    }

    override val contentType: String = stringContentType
}
```

Instead of serializing each argument to a string, it serializes each argument to a `JsonElement`, and then combines them
into a `JsonObject`. This could also be done by defining a `Serializer<T, JsonElement>` and the
corresponding `SerializedFormatTransformer<JsonElement>`, but that is unnecessary here.

Note the use
of [`ArgumentSerializers`](https://rnett.github.io/krosstalk/release/core/krosstalk/-krosstalk/com.rnett.krosstalk.serialization.plugin/-argument-serializers/index.html)
, which is essentially a `{Argument -> Serializer}` with helper functions to get serializers and serialize maps.

## Client

For an example, see [ktor-client](./plugins/ktor-client). The main plugin is
[krosstalk-ktor-client](./plugins/ktor-client/krosstalk-ktor-client), but artifacts are split out to match Ktor's
artifact structure. This guide will follow its implementation.

To define a client, you will need to define a scope for your plugin that extends `ClientScope` (and generally preserves
the type parameter) and a `ClientHandler<S>` where `S` is your scope. You will also genrally want to define an interface
implementing `KrosstalkClient` that uses your scope and client handler classes. Examples from the Ktor client plugin
(definitions only):

```kotlin
public class KtorClient : ClientHandler<KtorClientScope<*>>

public interface KtorClientScope<in D> : ClientScope<D>

public interface KtorKrosstalkClient : KrosstalkClient<KtorClientScope<*>> {
    override val client: KtorClient
}
```

The client handler must define a single method:
`suspend fun sendKrosstalkRequest(url: String, httpMethod: String, contentType: String, additionalHeaders: Headers, body: ByteArray?, scopes: List<AppliedClientScope<C, *>>,): InternalKrosstalkResponse`
(`C` is the scope type of the `ClientHandler`), which sends the actual HTTP request and returns a
[`InternalKrosstalkResponse`](https://rnett.github.io/krosstalk/release/core/krosstalk-client/-krosstalk%20-client/com.rnett.krosstalk.client.plugin/-internal-krosstalk-response/index.html#properties)
, which encapsulates the response code, headers, body, and how to get the body as a string. The parameters are mostly
self explanatory: `url` is the url to send the request to, `httpMethod` is the HTTP method to use, `contentType` is the
content type of the request, `additionalHeaders` are additional headers to add to the request, and `body` is the request
body (or `null` if it is empty).  `scopes` is a list of scopes applied to the method. Remember, from the Scopes section
of the readme, that client scopes take some data and apply it to the request (thus the `in` type parameter).
`AppliedScope` is a helper class that contains the applied `scope` and the `data` it was applied with. Each of these
scopes ie enforced (at compile time) to be of the client handler's defined scope type. How exactly a scope is applied to
a request depends on the client implementation, but your scope interface should define the appropriate methods to be
overridden by scopes and called by the client handler.

For the Ktor client, the scope class `KtorClientScope` looks like
([source](./plugins/ktor-client/krosstalk-ktor-client/src/commonMain/kotlin/com/rnett/krosstalk/ktor/client/Scopes.kt#L16)):

```kotlin
public interface KtorClientScope<in D> : ClientScope<D> {
    public fun HttpClientConfig<*>.configureClient(data: D) {}
    public fun HttpRequestBuilder.configureRequest(data: D) {}
}
```

These methods are used from helper functions
([source](./plugins/ktor-client/krosstalk-ktor-client/src/commonMain/kotlin/com/rnett/krosstalk/ktor/client/Client.kt#L25)):

```kotlin
internal fun <D> AppliedClientScope<KtorClientScope<D>, *>.configureClient(client: HttpClientConfig<*>) {
    client.apply {
        scope.apply { configureClient(data as D) }
    }
}

internal fun <D> AppliedClientScope<KtorClientScope<D>, *>.configureRequest(request: HttpRequestBuilder) {
    request.apply {
        scope.apply { configureRequest(data as D) }
    }
}
```

The client handler itself then looks like
([source](./plugins/ktor-client/krosstalk-ktor-client/src/commonMain/kotlin/com/rnett/krosstalk/ktor/client/Client.kt#L54)):

```kotlin
public class KtorClient(
    public val baseClient: HttpClient = HttpClient(),
    public val baseRequest: HttpRequestBuilder.() -> Unit = {},
) : ClientHandler<KtorClientScope<*>> {

    private val realBaseClient by lazy {
        baseClient.config {
            expectSuccess = false
        }
    }

    override suspend fun sendKrosstalkRequest(
        url: String,
        httpMethod: String,
        contentType: String,
        additionalHeaders: com.rnett.krosstalk.Headers,
        body: ByteArray?,
        scopes: List<AppliedClientScope<KtorClientScope<*>, *>>,
    ): InternalKrosstalkResponse {
        // configure the client and make the request
        val response = realBaseClient.config {
            scopes.forEach {
                it.configureClient(this)
            }
        }.use { client ->
            client.request<HttpResponse>(urlString = url) {
                if (body != null)
                    this.body = body
                this.method = HttpMethod(httpMethod.toUpperCase())

                // base request configuration
                baseRequest()

                // configure scopes
                scopes.forEach {
                    it.configureRequest(this)
                }

                // add any set headers
                additionalHeaders.forEach { (key, list) ->
                    this.headers.appendAll(key, list)
                }
            }
        }

        val bytes = response.receive<ByteArray>()
        val charset = response.charset() ?: Charsets.UTF_8

        return InternalKrosstalkResponse(response.status.value, response.headers.toMap(), bytes) {
            String(
                bytes,
                charset = charset
            )
        }
    }
}
```

## Server

For an example, see [ktor-server](./plugins/ktor-server). The main plugin is
[krosstalk-ktor-server](./plugins/ktor-server/krosstalk-ktor-server), but artifacts are split out to match Ktor's
artifact structure. This guide will follow its implementation.

Similarly to clients, to define a server you will need to define a scope that extends `ServerScope` (note the `out`
variance of the type parameter in contract to `ClientScope`'s `in`) and a `ServerHandler<S>` where `S` is your scope
class. You will also generally want to define an interface implementing `KrosstalkServer` that uses your scope and
server handler classes. Examples from the Ktor server plugin (definitions only):

```kotlin
public object KtorServer : ServerHandler<KtorServerScope<*>>

public interface KtorServerScope<S : Any> : ServerScope<S>

public interface KtorKrosstalkServer : KrosstalkServer<KtorServerScope<*>> {
    override val server: KtorServer
}
```

Note that the server handler is an object, and `ServerHandler` doesn't define any methods. This is because servers
generally have their own entrypoints, and how you add endpoints may vary greatly. So we instead provide helper methods
to add a Krosstalk's methods to an already existing server. For Ktor, the definition looks like:

```kotlin
public fun <K> K.defineKtor(application: Application) where K : Krosstalk, K : KrosstalkServer<KtorServerScope<*>> {
    KtorServer.define(application.routing { }, this)
}
```

(it will eventually be migrated to multiple receivers)

Server plugins should provide documentation on how to register a Krosstalk object with their server implementation.

Similarly to clients, the server scope interface should define methods that scopes can override to modify request
handlers and extract the scope's data. For Ktor, this looks like
([source](./plugins/ktor-server/krosstalk-ktor-server/src/main/kotlin/com/rnett/krosstalk/ktor/server/Scopes.kt#L15)):

```kotlin
public interface KtorServerScope<S : Any> : ServerScope<S> {
    public fun Application.configureApplication() {}

    public fun Route.wrapEndpoint(optional: Boolean, endpoint: Route.() -> Unit) {}

    public fun getData(call: ApplicationCall): S?
}
```

These methods then must be used by our server handler's registration method. Since there is no method to override, the
registration method will generally get it's information from a `Krosstalk` object directly, using `Krosstalk. methods`
and `Krosstalk.serverScopes`.  `KrosstalkServer.scopesAsType` is also helpful to convert a method's list of scopes to
scopes of the plugin's type (this is enforced at compile time). A
method's `Endpoint` ([docs](https://rnett.github.io/krosstalk/release/core/krosstalk-base/-krosstalk%20-core/com.rnett.krosstalk.endpoint/-endpoint/index.html))
can be used for resolution. It has its own custom URL resolver that extracts parameters, called using `Endpoint.resolve`
. You can also get a resolution tree (`Endpoint.resolveTree`) or a list of all possible
resolutions (`Endpoint. allResolvePaths`) but neither of those have resolve methods (yet) so using `Endpoint.resolve` is
recommended.

However, most of the Krosstalk call handling is server independent, so we provide the `KrosstalkServer.handle`
method to handle the Krosstalk-specific logic
([docs](https://rnett.github.io/krosstalk/release/core/krosstalk-server/-krosstalk%20-server/com.rnett.krosstalk.server.plugin/handle.html)):

```kotlin
public typealias Responder = suspend (statusCode: Int, contentType: String?, responseHeaders: Headers, responseBody: ByteArray) -> Unit

public suspend fun <K> K.handle(
    serverUrl: String,
    method: MethodDefinition<*>,
    requestHeaders: Headers,
    urlArguments: Map<String, String>,
    requestBody: ByteArray,
    scopes: WantedScopes,
    handleException: (Throwable) -> Unit = { throw it },
    responder: Responder,
): Unit where K : Krosstalk, K : KrosstalkServer<*> 
```

This should be called by every server handler. The `Responder` passed should send a response from the server using its
parameters: the status code, content type (or null to not set), additional response headers, and the body.

To define Ktor's registration method, we first need a helper to wrap endpoints in scopes
([source](./plugins/ktor-server/krosstalk-ktor-server/src/main/kotlin/com/rnett/krosstalk/ktor/server/Server.kt#L32)):

```kotlin
internal fun wrapScopesHelper(
    route: Route,
    optional: Boolean,
    remaining: MutableList<KtorServerScope<*>>,
    final: Route.() -> Unit,
) {
    if (remaining.isEmpty())
        route.final()
    else {
        val scope = remaining.removeLast()

        scope.apply {
            route.wrapEndpoint(optional) {
                wrapScopesHelper(this, optional, remaining, final)
            }
        }
    }
}

internal fun wrapScopes(route: Route, optional: Boolean, remaining: List<KtorServerScope<*>>, final: Route.() -> Unit) =
    wrapScopesHelper(route, optional, remaining.toMutableList().asReversed(), final)
```

We also need a custom route selector using Krosstalk's Endpoint resolution as well as some attributes to store the
resolved URL arguments and the base url
([source](./plugins/ktor-server/krosstalk-ktor-server/src/main/kotlin/com/rnett/krosstalk/ktor/server/Server.kt#L164)):

```kotlin
private val KrosstalkMethodAttribute = AttributeKey<Map<String, String>>("KrosstalkMethodData")
private val KrosstalkMethodBaseUrlAttribute = AttributeKey<String>("KrosstalkMethodBaseUrl")

internal class KrosstalkRouteSelector(val method: MethodDefinition<*>) : RouteSelector(2.0) {
    override fun evaluate(context: RoutingResolveContext, segmentIndex: Int): RouteSelectorEvaluation {
        with(context) {
            if (call.request.httpMethod.value.toLowerCase() != method.httpMethod.toLowerCase()) {
                return RouteSelectorEvaluation.Failed
            }

            val prefix = segments.take(segmentIndex)

            val localUrl = UrlRequest(call.request.uri).withoutPrefixParts(prefix)

            val baseUrl =
                URLBuilder.createFromCall(call).buildString().substringBefore(localUrl.urlParts.joinToString("/"))

            val data = method.endpoint.resolve(localUrl) ?: return RouteSelectorEvaluation.Failed
            call.attributes.put(KrosstalkMethodAttribute, data)
            call.attributes.put(KrosstalkMethodBaseUrlAttribute, baseUrl)
            return RouteSelectorEvaluation(true, 2.0, segmentIncrement = segments.size - segmentIndex)
        }
    }

    override fun toString(): String {
        return "(Krosstalk method: ${method.name})"
    }
}
```

We can then define the registration method (note that this is defined in the `KtorServer` object for convenience)
([source](./plugins/ktor-server/krosstalk-ktor-server/src/main/kotlin/com/rnett/krosstalk/ktor/server/Server.kt#L84)):

```kotlin
public fun <K> define(base: Route, krosstalk: K) where K : Krosstalk, K : KrosstalkServer<KtorServerScope<*>> {
    // apply Application configuration for each defined scopes
    base.application.apply {
        krosstalk.serverScopes
            .forEach {
                it.apply {
                    configureApplication()
                }
            }
    }

    base.apply {

        // add each method
        krosstalk.methods.values.forEach { method ->
            this.createChild(KrosstalkRouteSelector(method)).apply {
                // wrap the endpoint in the needed scopes
                wrapScopes(
                    this,
                    false,
                    method.requiredScopes.let(krosstalk::scopesAsType)
                ) {

                    wrapScopes(
                        this,
                        true,
                        method.optionalScopes.let(krosstalk::scopesAsType)
                    ) {

                        handle {
                            val data = call.attributes[KrosstalkMethodAttribute]
                            val body = call.receiveChannel().toByteArray()

                            val scopes = MutableWantedScopes()

                            method.allScopes.let(krosstalk::scopesAsType).forEach { scope ->
                                scope.getData(call)?.let { scopes[scope as KtorServerScope<Any>] = it }
                            }

                            krosstalk.handle(call.attributes[KrosstalkMethodBaseUrlAttribute],
                                method,
                                call.request.headers.toMap(),
                                data,
                                body,
                                scopes.toImmutable(),
                                {
                                    application.log.error(
                                        "Server exception during ${method.name}, passed on to client",
                                        it
                                    )
                                }) { status: Int, contentType: String?, headers: Headers, bytes: ByteArray ->

                                headers.forEach { (k, v) ->
                                    v.forEach {
                                        call.response.headers.append(k, it, false)
                                    }
                                }

                                call.respondBytes(
                                    bytes,
                                    contentType?.let {
                                        try {
                                            ContentType.parse(it)
                                        } catch (t: BadContentTypeFormatException) {
                                            null
                                        }
                                    },
                                    HttpStatusCode.fromValue(status)
                                )
                                this.finish()
                            }
                        }
                    }
                }
            }
        }
    }
}
```

Note the use
of `MutableWantedScopes` ([docs](https://rnett.github.io/krosstalk/release/core/krosstalk/-krosstalk/com.rnett.krosstalk.server.plugin/-mutable-wanted-scopes/index.html?query=class%20MutableWantedScopes%20:%20WantedScopes))
to accumulate scope data. This is a utility class provided for just that purpose.


