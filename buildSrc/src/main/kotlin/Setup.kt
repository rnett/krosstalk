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
    optIn("kotlin.RequiresOptIn")
}

val hostOs: String get() = System.getProperty("os.name")
val isMingwX64 get() = hostOs.startsWith("Windows")
val isMacOs get() = hostOs == "Mac OS X"

val isMainHost get() = isMingwX64

@OptIn(ExperimentalStdlibApi::class)
inline fun KotlinMultiplatformExtension.allTargets(ktorRequired: Boolean = false) {
    jvm {
        //TODO remove once KT-36942 and KT-35003 are fixed
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 8)
        }
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
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

    val nativeTargets = when {
        isMacOs -> listOf(
            macosX64(),

            iosArm32(),
            iosArm64(),
            iosX64(),

            tvosArm64(),
            tvosX64(),

            watchosArm32(),
            watchosArm64(),
            watchosX86(),
            watchosX64(),
        ) + if(!ktorRequired) listOf(
            macosArm64(),

            iosSimulatorArm64(),
            tvosSimulatorArm64(),
            watchosSimulatorArm64(),
        ) else emptyList()
        hostOs == "Linux" -> listOf(
            linuxX64(),
            linuxArm32Hfp(),
//            linuxMips32(),
//            linuxMipsel32(),
        ) + if(!ktorRequired) listOf(
            linuxArm64(),
        ) else emptyList()
        isMingwX64 -> listOf(
            mingwX64(),
            mingwX86(),
        )
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    val publicationsFromMainHost = setOf("jvm", "js", "kotlinMultiplatform")

    sourceSets {
        val commonMain = getByName("commonMain")
        val commonTest = getByName("commonTest")

        val nativeMain = create("nativeMain") {
            dependsOn(commonMain)

            nativeTargets.map { it.name + "Main" }.forEach {
                sourceSets.getByName(it).dependsOn(this)
            }
        }

        val nativeTest = create("nativeTest") {
            dependsOn(commonTest)
            requiresVisibilityOf(nativeMain)

            nativeTargets.map { it.name + "Test" }.forEach {
                sourceSets.getByName(it).dependsOn(this)
            }
        }
    }

    sourceSets.all {
        languageSettings.commonSettings()
    }

    val project = nativeTargets.first().project
    with(project) {
        afterEvaluate {
            extensions.getByType<PublishingExtension>().apply {
                publications {
                    matching { it.name in publicationsFromMainHost }.all {
                        val targetPublication = this@all
                        tasks.withType<AbstractPublishToMaven>()
                            .matching { it.publication == targetPublication }
                            .configureEach {
                                onlyIf {
                                    isMainHost
                                }
                            }
                    }
                }
            }
        }
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

    val project = target.project
    with(project) {
        afterEvaluate {
            extensions.getByType<PublishingExtension>().apply {
                publications {
                    all {
                        val targetPublication = this@all
                        tasks.withType<AbstractPublishToMaven>()
                            .matching { it.publication == targetPublication }
                            .configureEach {
                                onlyIf {
                                    isMainHost
                                }
                            }
                    }
                }
            }
        }
    }
}