import org.gradle.api.GradleException
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.LanguageSettingsBuilder

inline fun LanguageSettingsBuilder.commonSettings() {
    useExperimentalAnnotation("kotlin.RequiresOptIn")
}

inline fun KotlinMultiplatformExtension.allTargets() {
    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
                useIR = true
            }
        }
    }

    js(IR) {
        browser()
        nodejs()
    }

    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    sourceSets.all {
        languageSettings.commonSettings()
    }
}

inline fun KotlinJvmProjectExtension.irAndJava8() {
    target {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
                useIR = true
            }
        }
        sourceSets.all {
            languageSettings.commonSettings()
        }
    }
}