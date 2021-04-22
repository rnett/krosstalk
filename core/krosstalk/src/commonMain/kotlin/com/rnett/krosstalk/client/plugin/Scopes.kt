package com.rnett.krosstalk.client.plugin

import com.rnett.krosstalk.KrosstalkPluginApi
import com.rnett.krosstalk.Scope


//TODO require KrosstalkPluginApi for implementing
/**
 * Since scopes are declared as objects, all scope classes should be open to allow for use.
 *
 * While delegation is possible, it is easy to delegate from an interface (i.e. [ClientScope]) that doesn't define any methods.
 * Thus, it is discouraged in favor of implementation.
 */
@KrosstalkPluginApi
public interface ClientScope<in D> : Scope

