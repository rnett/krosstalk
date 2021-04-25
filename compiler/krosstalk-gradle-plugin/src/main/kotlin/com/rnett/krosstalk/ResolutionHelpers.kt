package com.rnett.krosstalk

import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.HasConfigurableAttributes
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget

enum class KrosstalkType {
    Client, Server;
}

val krosstalkTypeAttribute = Attribute.of("com.rnett.krosstalk.type", KrosstalkType::class.java)

//TODO document these somewhere, example w/ microservices

fun <T : HasConfigurableAttributes<*>> T.krosstalkServer() = apply {
    attributes { it.attribute(krosstalkTypeAttribute, KrosstalkType.Server) }
}

fun <T : HasConfigurableAttributes<*>> T.krosstalkClient() = apply {
    attributes {
        it.attribute(krosstalkTypeAttribute, KrosstalkType.Client)
    }
}

fun <T : KotlinTarget> T.krosstalkServer() = apply {
    attributes {
        attribute(krosstalkTypeAttribute, KrosstalkType.Server)
    }
}

fun <T : KotlinTarget> T.krosstalkClient() = apply {
    attributes {
        attribute(krosstalkTypeAttribute, KrosstalkType.Client)
    }
}
