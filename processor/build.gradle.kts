plugins {
    id("kbuild.jvm")
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(libs.bundles.processor)
    implementation(libs.autoservice.annotations)
    ksp(libs.autoservice.ksp)
}