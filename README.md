# Krosstalk: A pure Kotlin pluggable RPC library

[![Maven Central](https://img.shields.io/maven-central/v/com.github.rnett.krosstalk/krosstalk)](https://search.maven.org/artifact/com.github.rnett.krosstalk/krosstalk)
[![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/com.github.rnett.krosstalk/krosstalk?server=https%3A%2F%2Foss.sonatype.org)](https://oss.sonatype.org/content/repositories/snapshots/com/github/rnett/krosstalk/)
[![GitHub Repo](https://img.shields.io/badge/GitHub-Krosstalk-blue?logo=github)](https://github.com/rnett/krosstalk)
[![License](https://img.shields.io/badge/License-Apache%202.0-yellowgreen.svg)](https://opensource.org/licenses/Apache-2.0)
[![Changelog](https://img.shields.io/badge/Changelog-CHANGELOG.md-green)](https://github.com/rnett/krosstalk/blob/main/CHANGELOG.md#changelog)

Krosstalk allows you to easily create RPC methods using pure kotlin. Client, server, and serialization implementations
are pluggable, and Kotlin's `expect/actual` modifiers can be used to ensure that client and server methods match.

Ktor client and server and Kotlinx Serialization plugins are provided, along
with [instructions on how to write your own](./WRITING_PLUGINS.md).

### Compatibility

Krosstalk is currently not compatible with Compose in the same module, because their compiler plugin does some weird
stuff. To get around this, put all your Krosstalk stuff in other modules and depend on it in the modules with the
compiler plugin applied.
[Tracked issue](https://issuetracker.google.com/issues/185609826).

## Artifacts

### The compiler plugin, which is loaded via the gradle plugin below, is required for anything to work!

### Core ([Docs](https://rnett.github.io/krosstalk/release/core/index.html))

[Snapshot Docs](https://rnett.github.io/krosstalk/snapshot/core/index.html)

* Gradle plugin (**required**): `com.github.rnett.krosstalk` for the `plugins` block. Full coordinates
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

Plugins provide handlers for serialization, clients, or servers that can be used in Krosstalk objects.
They control how your Krosstalk methods are actually executed.

#### Serialization

* Kotlinx serialization: `com.github.rnett.krosstalk:krosstalk-kotlinx-serialization`
    * Includes JSON dependency, works with all formats.

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

Client (i.e. JS):

```kotlin
actual object MyKrosstalk : Krosstalk(), KtorKrosstalkClient {
    actual override val serialization = KotlinxBinarySerializationHandler(Cbor { })
    override val serverUrl: String = "http://localhost:8080"

    override val client = KtorClient()
}

actual suspend fun basicTest(data: Data): List<String> = krosstalkCall()
```

Server (i.e. JVM):

```kotlin
actual object MyKrosstalk : Krosstalk(), KtorKrosstalkServer {
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

Note that clients and servers can be on any platform that has the needed plugins.

The projects in [tests](./tests) function as good examples of more advanced behavior, 
including use for microservices and a client-only example that calls a normal API.

# Overview

Krosstalk works by registering any methods annotated with `@KrosstalkMethod` with the class specified in the annotation,
which must be an `object` that extends `Krosstalk` (referred to as the Krosstalk object), and **must be in the same module**. Client methods (which must
have a body of `krosstalkCall()`) will then have their bodies replaced with a call to the Krosstalk object, which will
use the client handler to send a request and return the response). Registering the server Krosstalk with your server
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
  between your Krosstalk object and `Krosstalk` to define these.  You can get serialization handlers from serialization plugins.
* Client Krosstalks are those that implement `KrosstalkClient` in addition to `Krosstalk`. They can be declared as
  standalone objects, or as the `actual` object of a common Krosstalk. They specify a client handler via the
  `client` property and a server url via the `serverUrl` property. The server url is read each request, so it can
  be `var`, but
  using [server url parameters](https://rnett.github.io/krosstalk/release/core/krosstalk/-krosstalk/com.rnett.krosstalk.annotations/-server-u-r-l/index.html)
  is recommended instead.  You get handlers from client plugins.
* Server Krosstalks are those that implement `KrosstalkServer` in addition to `Krosstalk`. Like clients, than can be
  standalone or `actual`. They define a server handler via `server`. This handler is usually just an object, since
  server entrypoints and structure can vary, but should define methods to add your Krosstalk's methods to its server
  implementation (i.e. `defineKtor`).  You get handlers from server plugins.

The `KrosstalkClient` and `KrosstalkServer` interfaces also require you to provide the scope class of your client or
server plugin, respectively. Plugins usually define their own interface or typealias that does this, i.e.
`KtorKrosstalkClient`. Scopes are a mechanism to allow clients to modify requests depending on arguments and to allow
servers to selectively match and extract data from requests. As such, their implementation will be plugin specific,
requiring you to specify the scope type. See the [Scope section](#scopes) for details.

## Methods

As mentioned before, Krosstalk methods are methods annotated with `KrosstalkMethod`, which must be passed the Krosstalk
object to register them with. Krosstalk methods must also be `suspend` (because they will be converted to a HTTP
request), and all parameters (including receivers) and the return type should be serializable by the serialization
handler of their Krosstalk object. Methods can be further configured using the annotations in
`com.rnett.krosstalk.annotations`. All configuration (including the `KrosstalkMethod` annotation) should be done on the
`expect` methods when using a common Krosstalk. I suggest reading
over [the docs of `com.rnett.krosstalk. annotations`](https://rnett.github.io/krosstalk/release/core/krosstalk/-krosstalk/com.rnett.krosstalk.annotations/index.html)
for the details (click on an annotation to see the full docs) of what each annotation does; an overview of what is
possible is provided below. Looking over
[the common tests](tests/fullstack-test/src/commonMain/kotlin/com/rnett/krosstalk/fullstack_test/Test.kt) and
[the client tests](tests/client-test/src/jsMain/kotlin/com/rnett/krosstalk/client_test/Test.kt) should give you an idea
of what is possible, too.

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

### Endpoints

The endpoint a method uses (the relative URL and the HTTP method) can be configured using `@KrosstalkEndpoint`. The
annotation takes the HTTP method and content type, and a template for the endpoint (which should be a relative URL).  
The template allows parameters to be used in the endpoint, using the following syntax:

* Literals: text, plus `$krosstalkPrefix` to use the Krosstalk object's prefix value, and `$methodName` to use the
  method's name plus the argument hash unless disabled in `@KrosstalkMethod`.
* Parameter: `{name}` - encodes the value of the parameter with that name.
* Parameter with name: `{{name}}` - is desugared into `/name/{name}` or `name={name}` depending on the URL region.
* Optional: `[?name:...]` - Evaluates to the body (`...`) if `name` is present (not null if `name` is `@Optional` or not
  a default if `name` is `@ServerDefault`), otherwise is empty. The body must be complete segments (i.e. between `/`s in
  the body, or complete `key=value` int he tailcard).  `name` must be `@Optional` or `@ServerDefault`.
* Optional parameter with name: `{{?name}}` - desugars to `[?name:{{name}}]`.  `name` must be `@Optional`
  or `@ServerDefault`.

Parameter names must be a parameter of the method, `$instanceReceiver` if an instance/dispatch receiver is present, or
`$extensionReceiver` if an extension receiver is present.  **Endpoint templates are checked for correctness at compile
time.**  You can't use "special" parameters (`@Ignored`, `@RequestHeaders`, `@ServerURL`) in the endpoint.

The default endpoint is `$krosstalkPrefix/$methodName`. A standard example
is `$krosstalkPrefix/$methodName/?{{a}}&{ {b}}` aka `$krosstalkPrefix/$methodName/?a={a}&b={b}`. Almost all endpoints
should start with `$krosstalkPrefix/$methodName`.

Note that in all strings here, the `$` should be escaped in Kotlin. However, we provide constants with the same names,
so you can safely use `$krosstalkName` or `\$krosstalkName`, as long as the first one resolves to our constant.

URL parameters are serialized using the Krosstalk object's `urlSerialization`, which is `serialization` by default.

Parameters that are in the URL whenever they are not null will be passed only there and not in the body.

`@EmptyBody` ensures that all parameters are passed in the URL, and is required to use the `GET` method unless there are
no parameters.

`contentType` may be empty, in which case the serialization handler's content type is used.

### Optionals/Defaults

Optionals and default values can be handled in two ways, both requiring `@Optional` on the parameter: nullable
and `ServerDefault`, which require a nullable or `ServerDefault` type, respectively. Nullables are easier to use, but
uses `null` to encode "not present", and so doesn't work for nullable data.  `ServerDefault` does.

Nullable and `ServerDefault` `@Optional`s are "present" when a non-null or non-default value is specified,
respectively.  
They can be used in endpoints (and as the predicate for optional blocks), but must not be used when they are not
present (i.e. they must be gated behind an optional).

When `@Optional` parameters are not present, they are not passed at all, and the server uses
`null` or the default, respectively.

`ServerDefault` is essentially an optional type, but the
`None` is hidden. The compiler plugin will replace the default value of the parameter with `None` on the client side,
which will lead to it not being passed and the default being evaluated on the server.

### Objects

Object parameters, receivers, and return values are not passed by default.  **Note though, if the type of a parameter is
an abstract class, object subclasses will still be passed.**  This only happens when the compiler can prove that the
value will always be the same object. Objects are also ignored by the serialization logic, so not having serializers on
them will not cause issues.

This can be overridden using the `@PassObjects` annotation. By default, it only applies to parameters and receivers, but
if `returnToo` is true it applies to return values as well.

### Response Headers

To pass or get the response headers, return `WithHeaders<T>` and use `@RespondWithHeaders`. This wraps your result type
and adds a `Headers` object. Any headers set in the return value on the server will be added to the response, and the
value of the headers on the client will be parsed from the actual response headers.

This plays nice with `KrosstalkResult` for error handling. You can either have `WithHeaders<KrosstalkResult<T>>` to get
the headers on all responses, or `KrosstalkResult<WithHeaders<T>>` to only get the headers on success.

### Request Headers

To send request headers, mark a parameter of type `Headers` with `@RequestHeaders`. It can't be used in endpoints.  
The value on the server will be parsed from the actual request headers.

### Server URL

To send request headers, mark a parameter of type `String` or `String?` with `@ServerURL`. It can't be used in
endpoints. If the argument is `null`, the server url from the `krosstalkCall()` or the Krosstalk object will be used (in
that order of precedence).

The value on the server will be parsed from the actual server url. The value will depend on your server implementation (
and plugin) and hosting setup, so depending on it is unwise.

### Ignore

`@Ignore` can be applied to a nullable parameter or one with a default value. The argument then won't be passed (or
serialized), and `null` or the default will be used on the server, respectively.

This is mainly useful for parameters used in `krosstalkCall()`.

### `krosstalkCall()` arguments

The request headers, server url, and additional scopes can all be specified as arguments to `krosstalkCall()`. The
request headers will be added to any specified with `@RequestHeaders`. The server url will be overridden by any
non-null `@ServerURL` parameters, and fall back to the Krosstalk object's server url if null. Specifying any scopes that
are also specified by scope parameters will result in a runtime error when the method is called.

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

## Resolution helpers

Microservices are a rather common use case of RPC, and to better support them, we provide some resolution helpers,
visible in the [microservices test](tests/microservices-test). These help manage dependencies, and can be seen in the
test's [buildscripts](tests/microservices-test/ping/build.gradle.kts) (each microservice's server depends on the other's
client).

Generally, for a microservice, you will want to have a common module with the Krosstalk and data definitions, a client
module with just the Krosstalk client methods (and any other helpers for them), and a server module with the actual
implementation. Often, these will all be JVM modules (like in the test). However, Kotlin does not yet provide a good way
to distinguish between source sets when using `project()` dependencies. This is where our helpers come in.

To declare a module as the krosstalk client or server module, call `krosstalkClient()` or `krosstalkServer()` in its
Kotlin target configuration, respectively. Then, to depend on one or the other, use `project().krosstalkClient()
` or `.krosstalkServer()`, respectively. Gradle handles circular dependencies well, so having two microservices where
each's server depends on the other's client works well. Again, you can see this in action in the microservice test (the
buildscripts are the interesting part).

These both use the `com.rnett.krosstalk.krosstalkTypeAttribute` attribute, which needs to be [registered in the dependencies block](https://docs.gradle.org/current/userguide/variant_attributes.html#creating_attributes_in_a_build_script_or_plugin) in some cases.
**This is not done automatically**.
Which situations require this is not clear, but the most basic in-project use, as done in the [microservices test](tests/microservices-test), seems to work fine without it.
