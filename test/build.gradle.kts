import com.google.devtools.ksp.gradle.KspTask

plugins {
    id("kbuild.jvm")
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(project(":core"))
    ksp(project(":processor"))
}

tasks.withType<KspTask>()
    .configureEach {
        notCompatibleWithConfigurationCache("KSP has issues")
    }
