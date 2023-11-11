plugins {
    id("kbuild.multiplatform-library")
}

dependencies {
    commonMainApi(project(":core"))
    commonMainApi(libs.kotlinx.serialization.core)
}