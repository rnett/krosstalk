package com.rnett.krosstalk.server

import com.rnett.krosstalk.InternalKrosstalkApi
import com.rnett.krosstalk.KrosstalkException
import com.rnett.krosstalk.Scope

/**
 * Some required scopes are missing from a Krosstalk call.
 */
@OptIn(InternalKrosstalkApi::class)
public class MissingScopeException internal constructor(
    public val scope: Scope,
    public val methodName: String? = null
) : KrosstalkException(
    "Missing required scope $scope${methodName?.let { " for method $it" }.orEmpty()}."
)