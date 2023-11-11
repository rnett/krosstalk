plugins {
    id("kbuild.multiplatform")
}

kotlin {
    sourceSets.configureEach {
        languageSettings {
            explicitApi()
        }
    }
}