# Module Krosstalk Ktor Client

A basic multiplatform Krosstalk client plugin using [Ktor](https://ktor.io/).

To use, have your Krosstalk client object implement `KtorKrosstalkClient`, and set `client` to a `KtorClient`.

[KtorClient] can be passed a Ktor `HttpClient` to use, it tried to create one using `HttpClient()` by default.
The `baseRequest` parameter can be used to add shared request configuration.

The scope interface of this plugin is [KtorClientScope], which provides optional methods to configure the `HttpClient` (
i.e. adding features) and to configure requests (i.e. using features). A header based scope [KtorClientHeaderScope] is
provided as well.

An generic authentication scope and an implementation of that scope for basic auth are provided in the
[`krosstalk-ktor-client-auth`](../../krosstalk-ktor-client-auth) plugin.

# Package com.rnett.krosstalk.ktor.client

A basic Krosstalk client implementation using Ktor. See module description.