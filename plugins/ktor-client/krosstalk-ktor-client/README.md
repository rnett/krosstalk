# Module Krosstalk Ktor Client

A basic multiplatform Krosstalk client implementation using Ktor. Includes authentication scopes.

To use, have your Krosstalk client object implement `KtorKrosstalkClient`, and set `client` to a `KtorClient`.

[KtorClient] can be passed a Ktor `HttpClient` to use, it tried to create one using `HttpClient()` by default.
The `baseRequest` parameter can be used to add shared request configuration.

The scope interface of this plugin is [KtorClientScope], which provides optional methods to configure the `HttpClient` (
i.e. adding features) and to configure requests (i.e. using features). A header based scope [KtorClientHeaderScope] is
provided as well.

We also provide a base abstract auth scope [KtorClientAuth], and an implementation for Basic auth: [KtorClientBasicAuth]
.

# Package com.rnett.krosstalk.ktor.client

A basic Krosstalk client implementation using Ktor. See module description.