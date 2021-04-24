# Krosstalk

Krosstalk allows you to easily create RPC methods using pure kotlin. Client, server, and serialization implementations
are plugable, and Kotlin's expect-actual modifiers can be used to ensure that client and server methods match.

Ktor client and server and Kotlinx Serialization plugins are provided, along with instructions on how to write your own.

## Artifacts

### Core

* Gradle plugin (required): `com.rnett.krosstalk` for the `plugins` block. Full coordinates
  are `com.github.rnett.krosstalk:krosstalk-gradle-plugin`.
* Common:`com.github.rnett.krosstalk:krosstalk`
* Client: `com.github.rnett.krosstalk:krosstalk-client`
* Server: `com.github.rnett.krosstalk:krosstalk-server`

Any common source set defining an `expect` Krosstalk object will need to depend on the common artifact. The client and
server artifacts will generally be inherited from plugins.

### Plugins

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
