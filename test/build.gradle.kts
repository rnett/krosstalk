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
    implementation("io.ktor:ktor-client-encoding:2.3.6")

    testImplementation(libs.kotlinx.serialization.json)

    testImplementation(libs.ktor.server.test)

    testImplementation(libs.ktor.server.netty)
    testImplementation(libs.ktor.server.auth)
    testImplementation(libs.ktor.server.compression)

    testImplementation(libs.ktor.client.apache)
    testImplementation(libs.ktor.client.auth)
    testImplementation(libs.ktor.client.encoding)

    ksp(project(":processor"))
}

tasks.withType<KspTask>()
    .configureEach {
        notCompatibleWithConfigurationCache("KSP has CC issues")
    }
