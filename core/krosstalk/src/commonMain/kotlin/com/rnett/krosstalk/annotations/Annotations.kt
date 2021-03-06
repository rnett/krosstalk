package com.rnett.krosstalk.annotations

import com.rnett.krosstalk.Headers
import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.ServerDefault
import com.rnett.krosstalk.WithHeaders
import com.rnett.krosstalk.defaultEndpoint
import com.rnett.krosstalk.defaultEndpointHttpMethod
import com.rnett.krosstalk.extensionReceiver
import com.rnett.krosstalk.instanceReceiver
import com.rnett.krosstalk.krosstalkPrefix
import com.rnett.krosstalk.methodName
import com.rnett.krosstalk.result.KrosstalkResult
import com.rnett.krosstalk.result.runKrosstalkCatching
import com.rnett.krosstalk.result.throwOnHttpError
import com.rnett.krosstalk.result.throwOnServerException
import com.rnett.krosstalk.result.toKrosstalkResult
import com.rnett.krosstalk.serialization.plugin.SerializationHandler
import kotlin.reflect.KClass

/**
 * Can only be configured on the client methods.
 */
@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
internal annotation class ClientOnly

/**
 * Can only be configured on the top level Krosstalk function, i.e. the expect function
 * if using an expect Krosstalk.
 */
@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
internal annotation class TopLevelOnly

/**
 * Makes a method a krosstalk method.  Should only be on the `expect` declaration if using a common Krosstalk.
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
public annotation class KrosstalkMethod(val klass: KClass<out Krosstalk>, val noParamHash: Boolean = false)

/**
 * Specifies an endpoint for the krosstalk method to use.
 * [endpoint] should be a http-formatted string of the (relative) pathname and query string, i.e. `"/items/?id={id}"`.
 * Supported syntax is as follows:
 * * Literals
 * * Parameters: `{param}`.
 * * Parameter with name: `{{param}}`, becomes `/param/{param}` or `param={param}` depending on location.
 * * Optional: `[?param:...]`.  Evaluates to the body (`...`) if param is present (not null if `name` is a nullable `@Optional` or not
 *   a default if `name` is a `ServerDefault` `@Optional`), empty otherwise.  Contents are treated as full segments, i.e.
 *   `my[?param:id]={param2}` is not allowed.
 * * Optional named parameter: `{{?param}}`, becomes `{{param}}` if param is present, empty otherwise.
 *
 * For both optionals, `param` must either be `@Optional`.
 *
 * Valid parameter names are the method parameters, [instanceReceiver] if it has an instance/dispatch receiver,
 * [extensionReceiver] if it has a extension receiver.
 *
 * [krosstalkPrefix] and [methodName] may be used as literals, they evaluate to the Krosstalk's [Krosstalk.prefix] and the
 * method's name (including the signature hash), respectively.
 *
 * All 4 of these special parameters may be included by using the constant in string interpolation, like `"$methodName"`,
 * or by escaping the `$` like `"\$methodName"`.  This is because the value of each constant is it's name with `'$'` prepended.
 *
 * The default endpoint is thus `"$krosstalkPrefix/$methodName"`.
 *
 * URL parameters will be evaluated by serializing the argument using the Krosstalk's [Krosstalk.urlSerialization] (which by default is the same as the body).
 * I highly recommend setting it to something string-based like JSON.  If you want to include non-trivial functions of the arguments in the endpoint,
 * include them in the function as default arguments and use those.
 *
 * Parameters that are present in the URL whenever they are not null will not be passed in the body, and will instead be deserialized from the URL.
 *
 * [httpMethod] controls the HTTP method used by requests.  If it is `GET`, the function must have no parameters or use [EmptyBody].
 *
 * [contentType] controls the content type of the request and response.  If empty (which is default), the Krosstalk object's serialization handler's
 * [SerializationHandler.contentType] is used.  May not be used for non-success results when used with [ExplicitResult].  **This setting is a recommendation,
 * which may be ignored by the client in some cases.**  Servers should almost always use it, but may not in some corner cases.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@TopLevelOnly
public annotation class KrosstalkEndpoint(
    val endpoint: String = defaultEndpoint,
    val httpMethod: String = defaultEndpointHttpMethod,
    val contentType: String = "",
)

/**
 * Requires the method to always have an empty body, i.e. for use with GET.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@TopLevelOnly
public annotation class EmptyBody


/**
 * Pass object parameters like normal parameters.
 *
 * By default, arguments that are objects at the common level will not be passed (except for as URL parameters).
 * This annotation causes them to be passed like normal parameters.
 *
 * This doesn't apply to the return type unless [returnToo] is true (which it is not by default).
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@TopLevelOnly
public annotation class PassObjects(val returnToo: Boolean = false)

/**
 * Return a [KrosstalkResult], wrapping server exceptions or http errors.  Method return type must be a subclass of [KrosstalkResult].
 *
 * **The server side function should almost always use [runKrosstalkCatching] or [runCatching] and [Result.toKrosstalkResult]**.
 * To catch some exceptions as HTTP Errors, use `KrosstalkResult.catchAsHttpError` **on the server**.
 *
 * To only handle HTTP Errors or only handle server exceptions, return `KrosstalkResult.SuccessOrHttpError` or `KrosstalkResult.SuccessOrServerException`,
 * respectively.  These can easily be gotten by using [KrosstalkResult.throwOnServerException] and [KrosstalkResult.throwOnHttpError], respectively.
 *
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@TopLevelOnly
public annotation class ExplicitResult

/**
 * Controls the details of exception handling.
 *
 * If [propagateServerExceptions] is `true`, the server implementation will re-throw or somehow log any server exceptions **after
 * the call completes** (so the client will receive a [KrosstalkResult.ServerException] result).
 * Exactly what is done depends on the server implementation used, but at a minimum it should show up in logs.
 * Note that this occurs after any conversion of some server exceptions to http errors.
 *
 * [includeStacktrace] controls whether to include the stack trace of exceptions (via [Throwable.stackTraceToString]) in the
 * [KrosstalkResult.ServerException].  It may expose more information about the server than you want, so it is `false` by default.
 * A `false` value here will override any `true` values in [runKrosstalkCatching] or [Result.toKrosstalkResult].
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@TopLevelOnly
public annotation class ExceptionHandling(
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
public annotation class RespondWithHeaders

/**
 * Don't send a parameter if it is null (if nullable) or default (if ServerDefault).
 * Must be used on a nullable parameter or a parameter of type [ServerDefault].
 *
 * **If a nullable `@Optional` parameter is not present in a call to the server, `null` will be used, not any default.**
 *
 * Nullable `@Optional` parameters will have their defaults evaluated at the call side, and only not passed to the server if they are null.
 *
 * `ServerDefault` `@Optional` parameters must have a default value, and won't be passed to the server if their value is not specified (i.e.
 * the default is used).  In that case, the default will be evaluated on the server.
 *
 * If you want to use a default value when the parameter is not specified, make the parameter default to null and use `?:` in the server method body,
 * or use [ServerDefault].
 *
 * Optional parameters may be used in optional blocks in an endpoint (see [KrosstalkEndpoint]).
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@TopLevelOnly
public annotation class Optional

/**
 * Marks a `String` or `String?` parameter as the server url.  This will override the server url set in the krosstalk client if it is non null.
 *
 * This will override any server url passed to `krosstalkCall`.
 *
 * Note that this is the base URL of the server, not including any set endpoint or the [Krosstalk.prefix].
 *
 * Also note that the host seen by the server may not match the url the client sent to the request to, depending on your server configuration.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@TopLevelOnly
public annotation class ServerURL

/**
 * Marks a [Headers] parameter as the request headers.  The values passed to the client method will be
 * set as request headers when the request is made.  The value received on the server side is the headers
 * from the HTTP request.
 *
 * These headers will be set after scopes are applied, although most HTTP clients add headers rather than replacing them.
 *
 * These headers will be added to any passed to `krosstalkCall`.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@TopLevelOnly
public annotation class RequestHeaders

/**
 * Don't pass a parameter, and instead use `null` or the default on the server.
 * The marked parameter must be nullable or have a default value.
 *
 * You **can not** use ignored parameters in [KrosstalkEndpoint].
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@TopLevelOnly
public annotation class Ignore
