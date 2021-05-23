package com.rnett.krosstalk.client

import com.rnett.krosstalk.InternalKrosstalkApi
import com.rnett.krosstalk.KrosstalkPluginApi
import com.rnett.krosstalk.ScopeInstance
import com.rnett.krosstalk.client.plugin.ClientScope

@OptIn(KrosstalkPluginApi::class, InternalKrosstalkApi::class)
@Suppress("DEPRECATED")
public operator fun <T : ClientScope<C>, C> T.invoke(clientData: C): ScopeInstance<T> = ScopeInstance.Client(clientData, this)
