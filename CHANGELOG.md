# Changelog

## Next

*

## 1.1.1

Minor changes:

* [#11](https://github.com/rnett/krosstalk/pull/11) Kotlinx serialization handler: Use serializers from the format's
  `serializersModule`.

## 1.1.0

Breaking changes:

* [#6](https://github.com/rnett/krosstalk/pull/6) Remove `KotlinxJsonObjectSerializationHandler`, change all Kotlinx
  serialization handlers to work like it by default.
* [#5](https://github.com/rnett/krosstalk/pull/5) Make the server `handle` method `inline`, change the responder
  callback to accept the new `KrosstalkResult` class.

Minor changes:

* [#7](https://github.com/rnett/krosstalk/pull/7) Allow the use of `abstract` Krosstalk subclasses to define common
  configuration (the README already said it was possible, but it lied).
* [#1](https://github.com/rnett/krosstalk/pull/1) Properly throw a compiler error when trying to register a method with
  a Krosstalk class in another module. Would result in an ICE before.

Small unlisted changes to docs and CI.

## 1.0.0

The initial release.
