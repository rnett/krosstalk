import com.google.devtools.ksp.gradle.KspTask

plugins {
    id("kbuild.jvm-library")
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(libs.bundles.processor)
    implementation(libs.autoservice.annotations)
    ksp(libs.autoservice.ksp)
}

kotlin {
    explicitApi = null
}

tasks.withType<KspTask>()
    .configureEach {
        notCompatibleWithConfigurationCache("KSP has CC issues")
    }