# Changelog

## Next
* [#26](https://github.com/rnett/krosstalk/pull/26) Update Kotlin to 1.5.30.  Does not include the new Mac targets since Kotlinx serialization doesn't support them yet.

## 1.2.1
* [#25](https://github.com/rnett/krosstalk/pull/25) Update Ktor to 1.6.2 and Gradle to 7.1.1.  Enable watchOsX64 for Ktor plugins.

## 1.2.0

Breaking changes:
* [#22](https://github.com/rnett/krosstalk/pull/22) Update Ktor auth scopes.  Changes Basic to use Ktor's credential class and adds Bearer scopes.
  Only breaks if you used `com.rnett.krosstalk.ktor.client.auth.BasicCredentials` directly.

Minor changes:
* [#23](https://github.com/rnett/krosstalk/pull/23) Update Ktor to 1.6.1 and Seraialization to 1.2.2.  Does not enable watchOsX64 for Ktor plugins, 
  the issue being fixed was a false positive.
* [#24](https://github.com/rnett/krosstalk/pull/24) Update Kotlin to 1.5.21, coroutines to 1.5.1, and compiler-plugin utils to 1.0.2.

## 1.1.2

* [#19](https://github.com/rnett/krosstalk/pull/19) Update to Kotlin 1.5.20

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

