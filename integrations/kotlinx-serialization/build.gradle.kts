plugins {
    id("kbuild.multiplatform-library")
}

dependencies {
    commonMainApi(project(":core"))
    commonMainApi(libs.kotlinx.serialization.core)

    jvmTestImplementation(libs.kotlinx.serialization.json)
    jvmTestImplementation(libs.kotlinx.serialization.cbor)
}