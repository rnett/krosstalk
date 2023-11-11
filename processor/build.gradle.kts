plugins {
    id("kbuild.jvm-library")
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(libs.bundles.processor)
    implementation(libs.autoservice.annotations)
    ksp(libs.autoservice.ksp)
}