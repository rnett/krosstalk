plugins {
    id("kbuild.multiplatform-all-library")
}

dependencies {
    commonMainApi(project(":core"))
    commonMainApi(libs.kotlinx.serialization.core)

    jvmTestImplementation(libs.kotlinx.serialization.json)
    jvmTestImplementation(libs.kotlinx.serialization.cbor)
}