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

inline fun KotlinMultiplatformExtension.allTargets(project: Project, noWatchOsX64: Boolean = false) {
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

    val macPrefixes = setOf("macos", "ios", "tvos", "watchos")

    when {
        isMacOs -> {
            macosX64()

            iosArm32()
            iosArm64()
            iosX64()

            tvosArm64()
            tvosX64()

            watchosArm32()
            watchosArm64()
            watchosX86()

            if (!noWatchOsX64) {
                watchosX64()
            }
        }
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    val publicationsFromMainHost = setOf("jvm", "js", "kotlinMultiplatform", "native")

    sourceSets {
        if (isMacOs) {
            val commonMain = getByName("commonMain")

            val nativeMain = create("nativeMain") {
                dependsOn(commonMain)

                sourceSets.filter { sourceSet ->
                    macPrefixes.any { sourceSet.name.startsWith(it) }
                            && "Main" in sourceSet.name
                }.forEach {
                    it.dependsOn(this)
                }
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
                            .configureEach {
                                onlyIf { isMingwX64 }
                            }
                    }
                }
            }
        }

        sourceSets.all {
            languageSettings.commonSettings()
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