import com.google.devtools.ksp.gradle.KspTask

plugins {
    id("kbuild.jvm")
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinx.serialization)
}

dependencies {
    implementation(project(":core"))

    implementation(project(":integrations:ktor-server"))
    implementation(project(":integrations:ktor-client"))
    implementation(project(":integrations:kotlinx-serialization"))

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.client.apache)

    testImplementation(libs.ktor.server.test)

    ksp(project(":processor"))
}

tasks.withType<KspTask>()
    .configureEach {
        notCompatibleWithConfigurationCache("KSP has CC issues")
    }
