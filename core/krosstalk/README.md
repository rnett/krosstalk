# Module Krosstalk

Common Krosstalk APIs that are used be both clients and servers, and everything required to define an `expect`
Krosstalk. Includes `Krosstalk`, `Scope`, `KrosstalkResult`, etc.

Usually the only source set that needs to explicitly depend on this artifact is the `common` one, source sets or modules
with Krosstalk client or servers will inherit it.

The reason this artifact is separate is to allow for defining a common interface for the client and server.
Kotlin's `expect-actual` mechanism can be used to force client and server methods to have the same signature, and
configuration annotations can be placed on the `expect` methods.

For usage instructions, see the [github readme]($GITHUB_ROOT#readme).

# Package com.rnett.krosstalk

The main package. See module description.

# Package com.rnett.krosstalk.annotations

Annotations that can be used to configure Krosstalk methods.

# Package com.rnett.krosstalk.result

APIs for `KrosstalkResult`. See [the instructions on error handling]($GITHUB_ROOT#error-handling).

# Package com.rnett.krosstalk.serialization

Serialization APIs.

# Package com.rnett.krosstalk.serialization.plugin

APIs necessary to define serialization plugins. Look at the Kotlinx serialization plugin for examples.

For instructions on writing plugins, see [WRITING_PLUGINS.md](./../../WRITING_PLUGINS.md#writing-krosstalk-plugins).

# Package com.rnett.krosstalk.client

APIs that need to be accessible to core code, but are only used by clients.

# Package com.rnett.krosstalk.server

APIs that need to be accessible to core code, but are only used by servers.