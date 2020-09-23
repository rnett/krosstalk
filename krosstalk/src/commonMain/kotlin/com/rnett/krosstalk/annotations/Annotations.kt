package com.rnett.krosstalk.annotations

import com.rnett.krosstalk.Krosstalk
import kotlin.reflect.KClass

//@Target(AnnotationTarget.CLASS)
//@Retention(AnnotationRetention.BINARY)
//@MustBeDocumented
//annotation class KrosstalkHost

/**
 * Makes a method a krosstalk method.   Should only be on the `expect` declaration.
 *
 * By default, the endpoint will be "/krosstalk/$methodName" although the prefix can be overridden in Krosstalk.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
annotation class KrosstalkMethod(val klass: KClass<out Krosstalk>)


/**
 * Specifies an endpoint for the krosstalk method to use.
 * Can include arguments in the endpoint using `"{parameterName}"`. This will be evaluated with `toString()`.
 * These path arguments won't be used on the server side for evaluation (as everything is put in the request body),
 * but can be used to set endpoints based on arguments.
 * If you want to include non-trivial functions of the arguments in the endpoint, include them as default arguments and use the arguments.
 *
 * For instance and extension receivers, use `"{\$instance}"` and `"{\$extension}"`, respectively.
 * To include the name of the method, use `"{\$name}"`.
 * To include the preset set in the Krosstalk object, use `"{\$prefix}"` (it is not included automatically if [KrosstalkEndpoint] is present).
 *
 * The default endpoint is thus `"{\$prefix}/{\$name}"`.
 *
 * **Note:** don't include the `'\'` in the literals, it is there to ensure that the `'$'` doesn't get counted as a string template.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
annotation class KrosstalkEndpoint(val endpoint: String, val httpMethod: String = "POST")


/**
 * The listed scopes will be attached to this method's handler on the server, and applied to the request.  If they are not active, the request will not be sent and will error.
 * Should be on the `expect` declaration with [KrosstalkMethod].
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
annotation class RequiredScopes(vararg val scopes: String)

/**
 * The listed scopes will be attached to this method's handler on the server, and applied to the request if active.  If they are not active, the request will still be sent.
 * Should be on the `expect` declaration with [KrosstalkMethod].
 *
 * Note that these scopes will still always be applied to the server handler.  The optional vs required only applies to the client.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
annotation class OptionalScopes(vararg val scopes: String)