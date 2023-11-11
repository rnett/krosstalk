plugins {
    id("kbuild.multiplatform-library")
}

kotlin {
    sourceSets.configureEach {
        languageSettings {
            explicitApi()
        }
    }
}