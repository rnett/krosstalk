plugins {
    `kotlin-dsl`
    alias(libs.plugins.buildconfig)
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(libs.kgp)
}

buildConfig{
    packageName("")
    buildConfigField("String", "DOKKA_VERSIONING_MODULE",  "\"${libs.dokka.versioning.get().module}\"")
    buildConfigField("String", "DOKKA_VERSION",  "\"${libs.versions.dokka.get()}\"")
}