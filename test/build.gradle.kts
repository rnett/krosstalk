import com.google.devtools.ksp.gradle.KspTask

plugins {
    id("kbuild.multiplatform")
    alias(libs.plugins.ksp)
}

dependencies {
    commonMainImplementation(project(":core"))
    kspCommonMainMetadata(project(":processor"))
}



tasks.withType<KspTask>()
    .configureEach {
        notCompatibleWithConfigurationCache("KSP has issues")
    }