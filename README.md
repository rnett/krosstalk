# README is very out of date

# Krosstalk: Expect/Actual API call autowiring

[ ![Download](https://api.bintray.com/packages/rnett/krosstalk/krosstalk/images/download.svg) ](https://bintray.com/rnett/krosstalk/krosstalk/_latestVersion)
[![License](https://img.shields.io/badge/License-Apache%202.0-yellowgreen.svg)](https://opensource.org/licenses/Apache-2.0)

Krosstalk allows you to easily create RPC methods using pure kotlin. Client, server, and serialization implementations
are plugable, and Kotlin's expect-actual modifiers can be used to ensure that client and server methods match.

Ktor client and server and Kotlinx Serialization plugins are provided, along with instructions on how to write your own.

## Artifacts

### Core ([Docs](https://rnett.github.io/krosstalk/release/core/index.html))

[Snapshot Docs](https://rnett.github.io/krosstalk/snapshot/core/index.html)

* Gradle plugin (required): `com.rnett.krosstalk` for the `plugins` block. Full coordinates
  are `com.github.rnett.krosstalk:krosstalk-gradle-plugin`.
* Common:`com.github.rnett.krosstalk:krosstalk`
* Client: `com.github.rnett.krosstalk:krosstalk-client`
* Server: `com.github.rnett.krosstalk:krosstalk-server`

Any common source set defining an `expect` Krosstalk object will need to depend on the common artifact. The client and
server artifacts will generally be inherited from plugins.

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

# OLD

<span style="color: red; font-weight: bold">Currently blocked
by <a href="https://youtrack.jetbrains.com/issue/KT-44199">KT-44199</a>.</span>

Krosstalk is a Kotlin compiler plugin supported library that turns `expect`/`actual` methods into api calls and
endpoints, with fully pluggable serialization, client and server implementations. When a client method is called, the
arguments are serialized and sent to the server. The server deserializes the arguments, calls the method, and sends the
serialized result as the response. Note that this means any side-effects will only happen server side.

Krosstalk ships with Kotlinx serialization support by default, and a rudimentary Ktor client and server are available.

A working example is provided in `sample`.

## Basics

To use krosstalk methods, you need a Krosstalk object. This object is essentially a table of all known methods and
configuration options: client/server and serialization.

A basic krosstalk object using the Ktor client and server might look like:

**Common:**

```kotlin
expect object MyKrosstalk : Krosstalk
```

**JS (client):**

```kotlin
actual object MyKrosstalk : Krosstalk(), KrosstalkClient<KtorClientScope> {
    override val serialization = KotlinxSerializationHandler
    override val client = KtorClient
}
```

**JVM (server):**

```kotlin
actual object MyKrosstalk : Krosstalk(), KrosstalkServer<KtorServerScope>, Scopes {
    override val serialization = KotlinxSerializationHandler
    override val server = KtorServer
}
```

As you can see, each actual krosstalk object defines its serialization handler (`override val serialization`), whether
it is a client or server (inheriting from `KrosstalkClient` or `KrosstalkServer`, ignore the type argument for now), and
its client or server implementation (`override val client`, `override val server`). None of these have to match, so long
as they are compatible.

Using a krosstalk object is extremly simple:
**Common:**

```kotlin
@Serializable
data class Data(val num: Int, val str: String)

@KrosstalkMethod(MyKrosstalk::class)
expect suspend fun doThing(data: Data): List<String>
```

**JS (client):**

```kotlin
actual suspend fun doThing(data: Data): List<String> = krosstaklCall()
```

**JVM (server):**

```kotlin
actual suspend fun doThing(data: Data): List<String> {
    return List(data.num) { data.str }
}
```

The `expect` declaration should have a `@KrosstalkMethod` defining the krosstalk object to use and any scopes (I'll get
to that later). The JS `actual` declaration should only be the definition and `krosstaklCall()`. The JVM `actual`
declaration should have the implementation. All krosstalk methods must be `suspend`, as they are API calls even if they
don't look like it.

You also have to define the krosstalk endpoints in your server. Krosstalk won't create its own server, but any server
implementation *should* define helper functions to make this easy.

For the Ktor server, it looks like:

```kotlin
fun main() {
    embeddedServer(Netty, 8080, "localhost") {

        install(CORS) {
            anyHost()
        }
        KtorServer.define(this, MyKrosstalk)

        routing {
            // Index and JS resources
        }

    }.start(true)
}
```

The `KtorServer.define` defines a POST `krosstalk/$methodName` endpoint for each method and wraps them in their scope
handlers (again, later).

## Scopes

There is a rather glaring flaw with what I've covered so far: you can't authenticate your krosstalk methods. You also
can't do any other fancy frontend or backend handling. Scopes solve this.

A scope is a way of attaching special handling, optionally with required data, to a krosstalk method. They are defined
in the krosstalk object (or rather, right above it):

**Common:**

```kotlin
interface Scopes {
    val auth: ScopeHolder
}

expect object MyKrosstalk : Krosstalk, Scopes
```

The unfortunate requirement of using an interface is because Kotlin expect/actual force the property types to match
exactly, which, given that we want client scopes on the client and server scopes on the server, doesn't work.

Defining the client and server scopes looks like:
**JS (client):**

```kotlin
actual object MyKrosstalk : Krosstalk(), KrosstalkClient<KtorClientScope>, Scopes {
    override val serialization = KotlinxSerializationHandler
    override val client = KtorClient
    override val auth by scope<KtorClientAuth>()
}
```

**JVM (server):**

```kotlin
actual object MyKrosstalk : Krosstalk(), KrosstalkServer<KtorServerScope>, Scopes {
    override val serialization = KotlinxSerializationHandler
    override val server = KtorServer
    override val auth by scope(KtorServerAuth(mapOf("username" to "password")))
}
```

This is where the type parameter of `KrosstalkClient` and `KtorServerScope` come from: the client and server scopes need
to be supported by the client and server. A ktor server will only be able to support scopes that define handlers on ktor
requests, and the same for the client.

The `auth` scope is then used like:

```kotlin
@KrosstalkMethod(MyKrosstalk::class, "auth")
expect suspend fun doAuthThing(num: Int): Data
```

**JS (client):**

```kotlin
actual suspend fun doAuthThing(num: Int): Data = krosstaklCall()
```

**JVM (server):**

```kotlin
actual suspend fun doAuthThing(num: Int): Data {
    return Data(num, (num * 10).toString())
}
```

The inclusion of `"auth"` in `@KrosstalkMethod` specifies that it is a required scope for `doAuthThing`: the scope's
server handler will be attached to that endpoint.

## Implementing Serializers

## Implementing Clients

## Implementing Servers
