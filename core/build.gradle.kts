buildscript {
    dependencies {
        classpath("org.jetbrains.dokka:versioning-plugin:${Dependencies.dokka}")
    }
}

plugins {
    id("org.jetbrains.dokka")
}

val coreVersionDir: String? by project

val docs = preprocessDocs("README.md")

//TODO setup this and plugins:docs with the CI
tasks.withType<org.jetbrains.dokka.gradle.DokkaMultiModuleTask>().configureEach {
    this.fileLayout.set(org.jetbrains.dokka.gradle.DokkaMultiModuleFileLayout.CompactInParent)
    this.includes.from(docs)
    this.moduleName.set("Krosstalk")
    this.moduleVersion.set(version.toString())

    if (coreVersionDir != null && "snapshot" !in project.version.toString().toLowerCase()) {
        val oldVersionsDir = rootDir.resolve(coreVersionDir!!)
        println("Using older versions from $oldVersionsDir")
        pluginConfiguration<org.jetbrains.dokka.versioning.VersioningPlugin, org.jetbrains.dokka.versioning.VersioningConfiguration> {
            version = project.version.toString()
            olderVersionsDir = oldVersionsDir
        }
    }
}