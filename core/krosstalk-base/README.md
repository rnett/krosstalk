# Module Krosstalk Base

Krosstalk APIs that are shared between the runtime and compiler plugin. None of these are user facing, you should not
need them unless you are implementing a plugin.

Artifact: `com.github.rnett.krosstalk:krosstalk-base` (you shouldn't ever need this)

# Package com.rnett.krosstalk

Contains the default values for annotations, and the special parameter keys for endpoints (i.e. [extensionReceiver]).

Also has the base [KrosstalkException] and the [KrosstalkPluginApi] annotation.

# Package com.rnett.krosstalk.endpoint

Contains the endpoint template and resolution classes.

`Endpoint` represents a template from `@KrosstalkEndpoint`, with parameters and optional segments. Client plugins do not
need to work with it, it is all handled internally.

`EndpointResolveTree` provides a tree based URL resolver based on an endpoint, that can extract parameter values and
handle optionals. This is used by [Endpoint.resolve], which is generally how server plugins do method resolution. See
the Ktor server plugin for an example of how resolution can be handled.
