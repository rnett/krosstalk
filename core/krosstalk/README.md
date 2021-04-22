# Module Krosstalk

Common Krosstalk APIs that are used be both clients and servers, and everything required to define an `expect` Krosstalk. Includes `Krosstalk`, `Scope`, `KrosstalkResult`, etc.

Modules defining a `expect` Krosstalk that will have an `actual` client and server should depend on this.

The client and server artifacts expose this as an API, so explicitly depending on it is unnecessary.

# Package com.rnett.krosstalk

The main package.

# Package com.rnett.krosstalk.annotations

Annotations that can be used to configure Krosstalk methods.

# Package com.rnett.krosstalk.serialization

Serialization handling. Kotlinx serialization is included by default, but other serialization handlers can be defined.

# Package com.rnett.krosstalk.client

APIs that need to be accessible to common code, but are only used by clients.

# Package com.rnett.krosstalk.server

APIs that need to be accessible to common code, but are only used by servers.