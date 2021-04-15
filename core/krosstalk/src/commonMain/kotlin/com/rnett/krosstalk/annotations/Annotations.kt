package com.rnett.krosstalk.annotations

import com.rnett.krosstalk.Headers
import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.KrosstalkResult
import com.rnett.krosstalk.WithHeaders
import com.rnett.krosstalk.defaultEndpoint
import com.rnett.krosstalk.defaultEndpointHttpMethod
import com.rnett.krosstalk.instanceReceiver
import com.rnett.krosstalk.krosstalkPrefix
import com.rnett.krosstalk.runKrosstalkCatching
import com.rnett.krosstalk.serialization.SerializationHandler
import com.rnett.krosstalk.toKrosstalkResult
import kotlin.reflect.KClass


//TODO option to not send instance/extension(?) receiver when it is an object

// meta annotations
@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
internal annotation class ClientOnly

@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
internal annotation class TopLevelOnly

/**
 * Makes a method a krosstalk method.   Should only be on the `expect` declaration.
 *
 * By default, the endpoint will be "/krosstalk/$methodName" although the prefix can be overridden in Krosstalk.
 *
 * Ordinarily, a hash of the parameters will be included in the method name to support overloads.  This can be disabled
 * by setting [noParamHash] to true, but an compile time error will happen if this is set on a method with overloads that
 * are also krosstalk methods on the same class.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@TopLevelOnly
annotation class KrosstalkMethod(val klass: KClass<out Krosstalk>, val noParamHash: Boolean = false)

//TODO update docs
//TODO optional handling.  Make explicit?  Currently nullables don't get sent if optional in url, defaults aren't required on server
/**
 * Specifies an endpoint for the krosstalk method to use.
 * Should be a http-formatted string of the (relative) pathname and query string, i.e. `"/items/?id={id}"`.
 * Can include arguments in the endpoint using `"{parameterName}"`, **except scope parameters**.
 * This will be evaluated by serializing the argument using the Krosstalk's serialization.
 * I highly recommend you use a string serialization method ([SerializationHandler] with [String]) when using this.
 * Unless you specify [MinimizeBody] or [EmptyBody] the arguments used in the endpoint will still be passed in the body.
 * If you want to include non-trivial functions of the arguments in the endpoint, include them in the function as default arguments and use those.
 *
 * For instance and extension receivers, use `"$instanceReceiver"` and `"$extensionReceiver"`, respectively.
 * To include the name of the method, use `"$methodName"` (this includes the parameter hash if used).
 * To include the prefix set in the Krosstalk object, use `"$krosstalkPrefix` (it is not included automatically if [KrosstalkEndpoint] is present).
 *
 * The default endpoint is thus `"$krosstalkPrefix/$methodName"`.
 *
 * **Note:** The hardcoded constants can have the `'$'` escaped, or not, using string interpolation with the constants, such as [krosstalkPrefix] or [instanceReceiver],
 * whose value is `'$'` and their name.
 *
 * [httpMethod] controls the HTTP method used by requests.  If it is `GET`, the function must have no parameters or use [EmptyBody].
 *
 * [contentType] controls the content type of the request and response.  If empty (which is default), the Krosstalk object's serialization handler's
 * [SerializationHandler.contentType] is used.  May not be used for non-success results when used with [ExplicitResult].  **This is a recommendation,
 * which may be ignored by the client in some cases.**  Servers should almost always use it, but may not in some corner cases.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@TopLevelOnly
annotation class KrosstalkEndpoint(
    val endpoint: String = defaultEndpoint,
    val httpMethod: String = defaultEndpointHttpMethod,
    val contentType: String = "",
)

/**
 * Don't include arguments that are part of the [KrosstalkEndpoint] endpoint in the body, and error if all arguments aren't in the endpoint.
 * Those arguments will be passed in the endpoint.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@TopLevelOnly
annotation class EmptyBody


/**
 * By default, arguments that are objects at the common level will not be passed (except for as URL parameters).
 * This annotation causes them to be passed like normal parameters.
 *
 * This doesn't apply to the return type unless [returnToo] is true (which it is not by default).
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@TopLevelOnly
annotation class PassObjects(val returnToo: Boolean = false)

//TODO option to only do http errors, or only do exceptions (based on return type?) (should use separate result classes or sealed interfaces) (http error one should be usable wth CatchAsHttpError)
//TODO post 1.5: a version that uses kotlin.Result.  Would have to limit to http errors, can't serialize exceptions (test, can I have a custom serializable annotation?)

/**
 * Return a [KrosstalkResult], wrapping server exceptions or http errors.  Method return type must be [KrosstalkResult].
 * Server side function should return a [KrosstalkResult.Success].
 *
 * **The server side function should almost always use [runKrosstalkCatching] or [runCatching] and [Result.toKrosstalkResult]**.
 * To catch some exceptions as HTTP Errors, use [KrosstalkResult.catchAsHttpError] or [KrosstalkResult.catchAsHttpStatusCode] on the server.
 * To not catch some exceptions, use [KrosstalkResult.throwServerException] or [KrosstalkResult.throwOnServerException] to re-throw all.
 *
 * If [propagateServerExceptions] is `true`, the server implementation will re-throw or somehow log any server exceptions **after
 * the call completes** (so the client will receive a [KrosstalkResult.ServerException] result).
 * Exactly what is done depends on the server implementation used, but at a minimum it should show up in logs.
 * Note that this occurs after any conversion of some server exceptions to http errors.
 *
 * [includeStacktrace] controls whether to include the stack trace of exceptions (via [Throwable.stackTraceToString]) in the
 * [KrosstalkResult.ServerException].  It may expose more information about the server than you want, so it is `false` by default.
 * A `false` value here will override any `true` values in [runKrosstalkCatching] or [Result.toKrosstalkResult].
 *
 * Note that the response will not be a serialized [KrosstalkResult].  If successful, only the data will be serialized.  Server errors will respond
 * with a 500 status code with the exception data in the body, and http error codes will respond with their error code and message.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@TopLevelOnly
annotation class ExplicitResult(
    val includeStacktrace: Boolean = false,
    val propagateServerExceptions: Boolean = true,
)

/**
 * Allows the use of [WithHeaders] in the return type.
 * Must be the top level type, unless used with [KrosstalkResult], in which case it may be on the second level.
 *
 * Headers returned in the [WithHeaders] object on the server side will be added to the HTTP response.
 * On the client side, the returned [WithHeaders] object will have the headers from the HTTP response, (usually) including those set
 * in the server's returned [WithHeaders].  Note that most header sending implementations use case-insensitive names, so if you
 * return `{"My-Header": "Value"}` you may get `"my-header": "Value"` on the client.
 *
 * If your return type is `WithHeaders<KrosstalkResult<>>`, the response headers you return in the server function will only be set on success.
 * However, the return value on the client will still have the headers of any error responses.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@Repeatable
@TopLevelOnly
annotation class RespondWithHeaders()

/**
 * Must be used on a nullable parameter (for now).  If that parameter is null, it will not be sent.
 * **If it is not present in a call to the server, `null` will be used, not any default.**
 * Defaults will be evaluated at the call side, i.e. on client side if called from the client, or server side if called from the server.
 * Defaults are ignored for direct HTTP requests.
 *
 * If you want to use a default value when the parameter is not specified, make the parameter default to null and use `?:` in the server method body.
 *
 * Optional parameters may be used in optional blocks in an endpoint (see [KrosstalkEndpoint]).
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@TopLevelOnly
annotation class Optional()

//TODO see if I can replace this with passing the server url to krosstalkCall().  Would need an @Ignore or similar
/**
 * Marks a `String` or `String?` parameter as the server url.  This will override the server url set in the krosstalk client if it is non null.
 *
 * Note that this is the base URL of the server, not including any set endpoint or the [Krosstalk.prefix].
 *
 * Also note that the host seen by the server may not match the url the client sent to the request to, depending on your server configuration.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@TopLevelOnly
annotation class ServerURL

/**
 * Marks a [Headers] parameter as the request headers.  The values passed to the client method will be
 * set as request headers when the request is made.  The value received on the server side is the headers
 * from the HTTP request.
 *
 * These headers will be set after scopes are applied, although most HTTP clients add headers rather than replacing them.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@TopLevelOnly
annotation class RequestHeaders