package kbuild

plugins {
    id("kbuild.jvm")
}

kotlin {
    explicitApi()
    target {
        //TODO remove once KT-36942 and KT-35003 are fixed
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 8)
        }
        compilations.configureEach {
            kotlinOptions {
                jvmTarget = "1.8"
                //TODO remove once KT-36942 and KT-35003 are fixed
                compileJavaTaskProvider.configure {
                    targetCompatibility = "1.8"
                    sourceCompatibility = "1.8"
                }
            }
        }
    }
}