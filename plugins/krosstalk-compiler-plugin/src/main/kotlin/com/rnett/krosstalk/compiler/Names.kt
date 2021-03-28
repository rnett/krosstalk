package com.rnett.krosstalk.compiler

import com.rnett.plugin.naming.Class
import com.rnett.plugin.naming.ClassRef
import com.rnett.plugin.naming.PackageRef
import com.rnett.plugin.naming.RootPackage
import com.rnett.plugin.naming.constructor
import com.rnett.plugin.naming.function

const val krosstalkPackage = "com.rnett.krosstalk"
const val annotationPackage = "${krosstalkPackage}.annotations"

object Krosstalk : RootPackage(krosstalkPackage) {
    object Annotations : PackageRef() {
        val KrosstalkMethod by Class()
        val ClientOnly by Class()
        val TopLevelOnly by Class()
    }

    object Serialization : PackageRef() {
        val MethodTypes by Class()
    }

    object Client : PackageRef() {
        val KrosstalkClient by Class()
        val ClientScope by Class()
        val AppliedClientScope by Class()

        val instanceToAppliedScope by function("toAppliedScope")

        val call by function()
        val clientPlaceholder by function("krosstalkCall")
    }

    object Server : PackageRef() {
        val KrosstalkServer by Class()
        val ServerScope by Class()

        val createServerScopeInstance by function("invoke") {
            parameters[0] = {
                it.name.asString() == "serverData"
            }
        }

        object ImmutableWantedScopes : ClassRef() {
            val get by function()
            val getOptional by function()
        }
    }

    object KrosstalkResult : ClassRef() {
        object ServerException : ClassRef() {
            val throwableConstructor by constructor { numParameters = 2 }
        }
    }

    val ScopeHolder by Class()


    val getValueAsOrError by function()

    val Scope by Class()
    val ScopeInstance by Class()

    object Krosstalk : ClassRef() {
        val addMethod by function()
        val addScope by function()

    }

    object KrosstalkException : ClassRef() {
        val CallFromClientSide by Class()
    }

    val AnnotationSpec by Class()
    val KrosstalkAnnotations by Class()

}