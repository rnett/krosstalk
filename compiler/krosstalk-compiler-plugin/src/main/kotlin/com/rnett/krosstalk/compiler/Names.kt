package com.rnett.krosstalk.compiler

import com.rnett.plugin.naming.Class
import com.rnett.plugin.naming.ClassRef
import com.rnett.plugin.naming.PackageRef
import com.rnett.plugin.naming.RootPackage
import com.rnett.plugin.naming.function
import com.rnett.plugin.naming.primaryConstructor

const val krosstalkPackage = "com.rnett.krosstalk"
const val annotationPackage = "${krosstalkPackage}.annotations"

object Krosstalk : RootPackage(krosstalkPackage) {
    object Annotations : PackageRef() {
        val KrosstalkMethod by Class()
        val ClientOnly by Class()
        val TopLevelOnly by Class()
        val Optional by Class()
    }

    object Serialization : PackageRef() {
        val MethodTypes by Class()
    }

    object Client : PackageRef() {
        val KrosstalkClient by Class()

        object Plugin : PackageRef() {
            val ClientScope by Class()
            val AppliedClientScope by Class()

            val instanceToAppliedScope by function("toAppliedScope")
        }

        val call by function()
        val clientPlaceholderFqName = function("krosstalkCall").fqName
    }

    object Server : PackageRef() {
        val KrosstalkServer by Class()

        object Plugin : PackageRef() {
            val ServerScope by Class()

            object ImmutableWantedScopes : ClassRef() {
                val getRequiredInstance by function()
                val getOptionalInstance by function()
            }
        }

        val createServerScopeInstance by function("invoke") {
            parameters[0] = {
                it.name.asString() == "serverData"
            }
        }
    }

    object Result : PackageRef() {
        object KrosstalkResult : ClassRef() {
            //TODO allow returning any subclass of KrosstalkResult
            val Success by Class()
            val HttpError by Class()
            val ServerException by Class()

            val SuccessOrHttpError by Class()
            val SuccessOrServerException by Class()
            val Failure by Class()
        }
    }

    val Headers by Class()

    object WithHeaders : ClassRef() {
        val new by primaryConstructor()
    }

    object ServerDefault : ClassRef() {
    }

    val noneServerDefault by function()

    val ScopeHolder by Class()
    val getValueAsOrError by function()

    val Scope by Class()
    val ScopeInstance by Class()

    object Krosstalk : ClassRef() {
        val addMethod by function()
        val addScope by function()

    }

    val CallFromClientSideException by Class()
    val KrosstalkException by Class()

}