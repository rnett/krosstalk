# The Krosstalk Compiler Plugin

This is where the magic happens.

If you've moved things around in `core`, see [Names.kt](src/main/kotlin/com/rnett/krosstalk/compiler/Names.kt).

Otherwise,
see [KrosstalkMethodTransformer.kt](src/main/kotlin/com/rnett/krosstalk/compiler/KrosstalkMethodTransformer.kt).

We wrap Krosstalk objects in [KrosstalkClass], Krosstalk functions in [KrosstalkFunction], and their parameters
in [KrosstalkParameter]. Eventually these classes will be moved to their own files.


