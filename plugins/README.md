# Plugins

We provide plugins for [Kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) 
and [Ktor](https://ktor.io/) client and server.  Authentication scopes for Ktor clients and servers are also included.

If you are making a third party plugin, please make an issue or PR on the 
[GitHub repo](https://github.com/rnett/krosstalk) so that we can list it here.

## Artifacts

#### Serialization

* Kotlinx serialization (includes JSON): `com.github.rnett.krosstalk:krosstalk-kotlinx-serialization`

#### Client

* Ktor: `com.github.rnett.krosstalk:krosstalk-ktor-client`
    * Auth: `krosstalk-ktor-client-auth`

#### Server

* Ktor: `com.github.rnett.krosstalk:krosstalk-ktor-server`
    * Auth: `krosstalk-ktor-server-auth`
        * JWT: `krosstalk-ktor-server-auth-jwt`
