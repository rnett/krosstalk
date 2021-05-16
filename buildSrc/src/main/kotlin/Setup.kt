import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.attributes.java.TargetJvmVersion
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.LanguageSettingsBuilder

inline fun LanguageSettingsBuilder.commonSettings() {
    useExperimentalAnnotation("kotlin.RequiresOptIn")
}

fun Project.test() {
    this
}

inline fun KotlinMultiplatformExtension.allTargets(project: Project) {
    jvm {
        //TODO remove once KT-36942 and KT-35003 are fixed
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 8)
        }
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
                useIR = true
                //TODO remove once KT-36942 and KT-35003 are fixed
                compileJavaTaskProvider?.get()?.apply {
                    targetCompatibility = "1.8"
                    sourceCompatibility = "1.8"
                }
            }
        }
    }

    js(IR) {
        browser()
        nodejs()
    }

    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val isMacOs = hostOs == "Mac OS X"
    when {
        isMacOs -> {
            macosX64()
            ios()
            tvos()
            watchos()
        }
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    val publicationsFromMainHost = setOf("jvm", "js", "kotlinMultiplatform", "native")

    sourceSets {
        if (isMacOs) {
            val commonMain = getByName("commonMain")

            create("nativeMain") {
                dependsOn(commonMain)

                getByName("macosX64Main").dependsOn(this)
                getByName("iosMain").dependsOn(this)
                getByName("tvosMain").dependsOn(this)
                getByName("watchosMain").dependsOn(this)
            }
        }
    }

    with(project) {
        afterEvaluate {
            extensions.getByType<PublishingExtension>().apply {
                publications {
                    matching { it.name in publicationsFromMainHost }.all {
                        val targetPublication = this@all
                        tasks.withType<AbstractPublishToMaven>()
                            .matching { it.publication == targetPublication }
                            .configureEach { onlyIf { findProperty("isMainHost") == "true" } }
                    }
                }
            }
        }
    }

    sourceSets.all {
        languageSettings.commonSettings()
    }
}

inline fun KotlinJvmProjectExtension.irAndJava8() {
    target {
        //TODO remove once KT-36942 and KT-35003 are fixed
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 8)
        }
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
                useIR = true
                //TODO remove once KT-36942 and KT-35003 are fixed
                compileJavaTaskProvider.get().apply {
                    targetCompatibility = "1.8"
                    sourceCompatibility = "1.8"
                }
            }
        }
        sourceSets.all {
            languageSettings.commonSettings()
        }
    }
}