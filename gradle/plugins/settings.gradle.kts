rootProject.name = "plugins"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    versionCatalogs {
        create(defaultLibrariesExtensionName.get()) {
            from(files("../libs.versions.toml"))
        }
    }
}

include(
    "kotlin-base",
    "multiplatform",
    "jvm"
)

