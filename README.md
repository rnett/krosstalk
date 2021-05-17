# Krosstalk: Expect/Actual RPC call autowiring

![Maven Central](https://img.shields.io/maven-central/v/com.github.rnett.compiler-plugin-utils/compiler-plugin-utils)
[![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/com.github.rnett.compiler-plugin-utils/compiler-plugin-utils?server=https%3A%2F%2Foss.sonatype.org)](https://oss.sonatype.org/content/repositories/snapshots/com/github/rnett/compiler-plugin-utils/)
[![License](https://img.shields.io/badge/License-Apache%202.0-yellowgreen.svg)](https://opensource.org/licenses/Apache-2.0)

Krosstalk allows you to easily create RPC methods using pure kotlin. Client, server, and serialization implementations
are plugable, and Kotlin's expect-actual modifiers can be used to ensure that client and server methods match.

Ktor client and server and Kotlinx Serialization plugins are provided, along
with [instructions on how to write your own](./WRITING_PLUGINS.md).

## Artifacts

### Core ([Docs](https://rnett.github.io/krosstalk/release/core/index.html))

[Snapshot Docs](https://rnett.github.io/krosstalk/snapshot/core/index.html)

* Gradle plugin (required): `com.rnett.krosstalk` for the `plugins` block. Full coordinates
  are `com.github.rnett.krosstalk:krosstalk-gradle-plugin`.
* Core:`com.github.rnett.krosstalk:krosstalk`
* Client: `com.github.rnett.krosstalk:krosstalk-client`
* Server: `com.github.rnett.krosstalk:krosstalk-server`

Any common source set defining an `expect` Krosstalk object will need to depend on the core artifact. The client and
server artifacts will generally be inherited from plugins.

The Krosstalk compiler plugin will automatically not apply to source sets that don't
include `com.rnett.krosstalk. Krosstalk` as an accessible class (i.e. if you don't depend on any Krosstalk artifacts).

### Plugins ([Docs](https://rnett.github.io/krosstalk/release/plugins/index.html))

[Snapshot Docs](https://rnett.github.io/krosstalk/snapshot/plugins/index.html)

#### Serialization

* Kotlinx serialization (includes JSON): `com.github.rnett.krosstalk:krosstalk-kotlinx-serialization`

#### Client

* Ktor: `com.github.rnett.krosstalk:krosstalk-ktor-client`
  * Auth: `krosstalk-ktor-client-auth`

#### Server

* Ktor: `com.github.rnett.krosstalk:krosstalk-ktor-server`
  * Auth: `krosstalk-ktor-server-auth`
    * JWT: `krosstalk-ktor-server-auth-jwt`

## Minimal example:

Common:

```kotlin
@Serializable
data class Data(val num: Int, val str: String)

expect object MyKrosstalk : Krosstalk {
  override val serialization: KotlinxBinarySerializationHandler
}

@KrosstalkMethod(MyKrosstalk::class)
expect suspend fun basicTest(data: Data): List<String>
```

Client (JS):

```kotlin
actual object MyKrosstalk : Krosstalk(), KrosstalkClient<KtorClientScope<*>> {
  actual override val serialization = KotlinxBinarySerializationHandler(Cbor { })
  override val serverUrl: String = "http://localhost:8080"

  override val client = KtorClient()
}

actual suspend fun basicTest(data: Data): List<String> = krosstalkCall()
```

Server (JVM):

```kotlin
actual object MyKrosstalk : Krosstalk(), KrosstalkServer<KtorServerScope<*>> {
  actual override val serialization = KotlinxBinarySerializationHandler(Cbor { })
  override val server = KtorServer
}

actual suspend fun basicTest(data: Data): List<String> {
  return List(data.num) { data.str }
}

fun main() {
  embeddedServer(CIO, 8080, "localhost") {
    install(CORS) {
      anyHost()
    }
    MyKrosstalk.defineKtor(this)
  }.start(true)
}
```

# Overview

Krosstalk works by registering any methods annotated with `@KrosstalkMethod` with the class specified in the annotation,
which must be an `object` that extends `Krosstalk` (refered to as the Krosstalk object). Client methods (which must have
a body of `krosstalkCall()`) will then have their bodies replaced with a call to the Krosstalk object, which will use
the client handler to send a request and return the response). Registering the server Krosstalk with your server
implementation (using a server plugin) will then cause incoming requests to be handled by the Krosstalk object, which
will call the requested method and respond with the returned value.

There are three types of Krosstalk objects, depending on how they are declared: common, client, and server:

* Common Krosstalk objects are those that are declared `expect` in a common source set, and only extend `Krosstalk`. The
  primary reason for doing this is that you can then use it with `expect` Krosstalk methods, and Kotlin's
  `expect-actual`  mechanics will enforce that the client and server methods have exactly the same signature. To support
  this, Krosstalk's configuration must be done on `expect` Krosstalk methods when available. Common Krosstalks (and
  indeed all Krosstalks, since they all inherit from `Krosstalk`) must specify a serialization handler via
  `serialization`, and can optionally specify a different serialization handler for url arguments
  (`urlSerialization`) and a prefix to use in method endpoint urls (`prefix`, `"krosstalk"` by default).  **Note that
  the value of `prefix` and the serialization formats must match on `actual` client and server Krosstalks.**  
  It is not yet possible to declare these directly in the `expect` object, so take care. You can add a abstract class
  between your Krosstalk object and `Krosstalk` to define these.
* Client Krosstalks are those that implement `KrosstalkClient` in addition to `Krosstalk`. They can be declared as
  standalone objects, or as the `actual` object of a common Krosstalk. They specify a client handler via the
  `client` property and a server url via the `serverUrl` property. The server url is read each request, so it can
  be `var`, but
  using [server url parameters](https://rnett.github.io/krosstalk/release/core/krosstalk/-krosstalk/com.rnett.krosstalk.annotations/-server-u-r-l/index.html)
  is recommended instead.
* Server Krosstalks are those that implement `KrosstalkServer` in addition to `Krosstalk`. Like clients, than can be
  standalone or `actual`. They define a server handler via `server`. This handler is usually just an object, since
  server entrypoints and structure can vary, but should define methods to add your Krosstalk's methods to its server
  implementation (i.e. `defineKtor`).

The `KrosstalkClient` and `KrosstalkServer` interfaces also require you to provide the scope class of your client or
server plugin, respectively. Plugins usually define their own interface or typealias that does this, i.e.
`KtorKrosstalkClient`. Scopes are a mechanism to allow clients to modify requests depending on arguments and to allow
servers to selectively match and extract data from requests. As such, their implementation will be plugin specific,
requiring you to specify the scope type. More on that later.

## Methods

As mentioned before, Krosstalk methods are methods annotated with `KrosstalkMethod`, which must be passed the Krosstalk
object to register them with. Krosstalk methods must also be `suspend` (because they will be converted to a HTTP
request), and all parameters (including receivers) and the return type should be serializable by the serialization
handler of their Krosstalk object. Methods can be further configured using the annotations in
`com.rnett.krosstalk.annotations`. All configuration (including the `KrosstalkMethod` annotation) should be done on the
`expect` methods when using a common Krosstalk. I suggest reading
over [the docs of `com.rnett.krosstalk. annotations`](https://rnett.github.io/krosstalk/release/core/krosstalk/-krosstalk/com.rnett.krosstalk.annotations/index.html)
for an overview (click on an annotation to see the full docs).

### Error Handling

There are two separate modes for error handling: using exceptions, like normal, and using `@ExplicitResult` and
returning a subclass of `KrosstalkResult`. In both, throwing `KrosstalkHttpError` or `KrosstalkServerException`
(using `throwKrosstalkHttpError` and `throwKrosstalkServerException`) will result in an error response being sent, and
the exception being re-thrown on the client. However, methods using `@ExplicitResult` will usually include those in its
return value instead of throwing exceptions. In this cause, the error response will still be sent, but the exception
will be part of the client's return value like on the server instead of being re-thrown. In all cases,
`ServerException` responses are sent as 500s (with the `ServerException` being serialized to JSON), and `HttpError`
responses are sent with their status code and the message as the body.

If you throw an exception that is not `KrosstalkHttpError` or `KrosstalkServerException` **outside** of a
`runKrosstalkCatching` block or some other block that would wrap it in a `KrosstalkServerException`, **the behavior of
the method when called on the client and server will differ**. If called on the server, the original exception will be
thrown, while on the client, a `KrosstalkUncaughtServerException` wrapping the original exception will be thrown. This
is unavoidable, since exceptions can't be serialized.

If an exception is thrown during a call from a client, the server will handle it according to the settings in
`@ExceptionHandling`. If `propagateServerExceptions` is true, the underlying exception of any
`ServerException` (from throwing a `KrosstalkServerException` or returning one with `@ExplicitResult`) will be
propagated to your server handler's exception handler, which usually will at least log it.  `HttpError` results (via
exception or return) won't be logged anywhere specific, but almost all servers provide methods to log error responses,
and they will show up there. Any non-`KrosstalkServerException` or `KrosstalkHttpError` exceptions are re-thrown on the
server.  `includeStacktrace` controls whether the stack trace will be included in the responses. It is `false` by
default to prevent leaking details, and a `false` value here will override any `true` values in
`runKrosstalkCatching` or `throwKrosstalkServerException`. Note that it is impossible for a `true` value here to
override a `false` value elsewhere, so all other uses (i.e. `runKrosstalkCatching`) default to `true`. Also note that
this only applies on the client: the stack trace will always be present on the server.

When using `@ExplicitResult`, the entire method should almost always be wrapped in `runKrosstalkCatching`. Methods
like `KrosstalkResult<T>.catchAsHttpError` or `when` blocks can be used to convert `ServerException`s to
`HttpError`s.  `ServerException`s should generally be treated as an exceptional state, while `HttpError` is a less fatal
error. Helper methods like `KrosstalkResult<T>.handleHttpError` can be used to selectively handle error codes, so a
pattern like:

```kotlin
@KrosstalkMethod(MyKrosstalk::class)
@ExplicitResult
private expect fun _errorFunction(n: Int): KrosstalkResult<Int>

fun errorFunction(n: Int): Int? = _errorFunction(n)
  .throwOnServerException()
  .handleHttpError(404) { null }
  .valueOrThrow

// server
private actual fun _errorFunction(n: Int): KrosstalkResult<Int> = runKrosstalkCatching {
  ...
}.catchAsHttpError(NoSuchElementException::class, 404)
```

is fairly common. The server part can be omitted if the server code is changed to throw `KrosstalkHttpError`
instead of `NoSuchElementException`, i.e. by using `map[key] ?: throwKrosstalkHttpError(404)` instead
of `map.getValue(key)`.

## Scopes

Scopes are defined as nested `object`s in the Krosstalk objects. They must extend `Scope`, which is usually done
transitively. Note that for `actual` scopes this must be explicit because of
[KT-20641](https://youtrack.jetbrains.com/issue/KT-20641). Client and server scopes (declared in their respective
Krosstalk objects) must extend `ClientScope` and `ServerScope`, respectively. This is also usually done transitively,
since they also must extend their plugin's scope interface (or whatever was passed to `KrosstalkClient/Server`).
Both `ClientScope` and `ServerScope` have a type parameter; on `ClientScope`, this is the type of data that the scope
requires, on `ServerScope` it is the type of data the scope produces. The plugin's scope class will provide some methods
to override to configure the request, which should make use of the data on client scopes or have a way to produce it on
server scopes.
See [KtorClientScope](plugins/ktor-client/krosstalk-ktor-client/src/commonMain/kotlin/com/rnett/krosstalk/ktor/client/Scopes.kt)
and
[KtorServerScope](plugins/ktor-server/krosstalk-ktor-server/src/main/kotlin/com/rnett/krosstalk/ktor/server/Scopes.kt)
for examples.

Generally, you will not extend a plugin's scope class directly. Most plugins should provide scope classes to configure
the wanted behavior, like `KtorClientBasicAuth`. You can then have your scope class extend this.

Scopes are passed to methods by adding a parameter of type `ScopeInstance<T>`, where `T` is the scope's type. Scope
instances can be created using the `invoke` methods of `ClientScope` and `ServerScope`. If you need to create a scope
instance in common code (when using a common Krosstalk), you must use `expect-actual`, since the data types of the
scopes may differ between the client and server. To get the value extracted by the scope on the server, use
`ScopeInstance.value`.

Scope parameters can be made optional by making their type nullable (i.e. `ScopeInstance<T>?`). If this is the case,
`null` may be passed on the client resulting in no changes being made to the request, and the server may fail to get a
value (like if `null` was passed by the client, but not limited to this) resulting in the value of the argument
being `null`. This can be forbidden by some scope types by overriding `Scope.canBeOptional` and returning `false`.

Adding an authentication scope to our initial example would look like this:

Common:

```kotlin
@Serializable
data class Data(val num: Int, val str: String)

expect object MyKrosstalk : Krosstalk {
  override val serialization: KotlinxBinarySerializationHandler

  object Auth : Scope
}

@KrosstalkMethod(MyKrosstalk::class)
expect suspend fun basicTest(data: Data, auth: ScopeInstance<Auth>): List<String>
```

Client (JS):

```kotlin
actual object MyKrosstalk : Krosstalk(), KrosstalkClient<KtorClientScope<*>> {
  actual override val serialization = KotlinxBinarySerializationHandler(Cbor { })
  override val serverUrl: String = "http://localhost:8080"

  override val client = KtorClient()

  actual object Auth : Scope, KtorClientBasicAuth()
}

actual suspend fun basicTest(data: Data, auth: ScopeInstance<Auth>): List<String> = krosstalkCall()
```

Server (JVM):

```kotlin
actual object MyKrosstalk : Krosstalk(), KrosstalkServer<KtorServerScope<*>> {
  actual override val serialization = KotlinxBinarySerializationHandler(Cbor { })
  override val server = KtorServer

  actual object Auth : Scope, KtorServerBasicAuth<User>("auth") {
    override fun BasicAuthenticationProvider.Configuration.configure() {
      validate {
        if (validUsers[it.name] == it.password)
          User(it.name)
        else
          null
      }
    }
  }
}

data class User(val username: String) : Principal

private val validUsers = mapOf("username" to "password")

actual suspend fun basicTest(data: Data, auth: ScopeInstance<Auth>): List<String> {
  println("Request from: ${auth.value/*: User*/.username}")
  return List(data.num) { data.str }
}

fun main() {
  embeddedServer(CIO, 8080, "localhost") {
    install(CORS) {
      anyHost()
    }
    MyKrosstalk.defineKtor(this)
  }.start(true)
}
```

