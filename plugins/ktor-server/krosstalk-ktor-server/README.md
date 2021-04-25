# Module Krosstalk Ktor Server

A basic Krosstalk server plugin using [Ktor](https://ktor.io/). Includes authentication scopes.

To use, have your Krosstalk server object implement `KtorKrosstalkServer`, and set `server` to a `KtorServer`.

You can then add your Krosstalk's methods to a Ktor server using [defineKtor].

The scope interface of this plugin is [KtorServerScope], which provides optional methods to configure the `HttpServer` (
i.e. adding features), to wrap methods in routing (i.e. using features), and extract the scope data from
the [ApplicationCall]. A header based scope [KtorServerHeaderScope] is provided as well.

An generic authentication scope, another one for `Principal` producing authentications, and implementations for Ktor's
authentication types are provided in the `krosstalk-ktor-server-auth` plugin. JWT auth has its own
plugin: `krosstalk-ktor-server-auth-jwt`.

# Package com.rnett.krosstalk.ktor.server

A basic Krosstalk server implementation using Ktor. See module description.