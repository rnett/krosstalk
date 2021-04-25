# Krosstalk Core

These docs are for the core artifacts of Krosstalk:

* `krosstalk-core`: APIs shared between the runtime and compiler plugin
* `krosstalk`: The common API.
* `krosstalk-server`: The server API.
* `krosstalk-client`: The client API.

Note that despite calling `krosstalk` "the common API", both the client and server APIs are fully multiplatform.
However, generally your common source set will be the only one depending on `krosstalk` directly.

A guide to Krosstalk is available in the [GitHub README](./../README.md), and each artifact's docs give an overview of
how to use it (the guide should be read first).
