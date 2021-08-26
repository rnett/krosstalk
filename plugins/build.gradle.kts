buildscript {
    dependencies {
        classpath("org.jetbrains.dokka:versioning-plugin:1.5.0")
    }
}

plugins {
    id("org.jetbrains.dokka")
}

val pluginVersionDir: String? by project

val docs = preprocessDocs("README.md")

tasks.withType<org.jetbrains.dokka.gradle.DokkaMultiModuleTask>().configureEach {
    this.fileLayout.set(org.jetbrains.dokka.gradle.DokkaMultiModuleFileLayout.CompactInParent)
    this.includes.from(docs)
    this.moduleName.set("Krosstalk Plugins")
    this.moduleVersion.set(version.toString())

    if (pluginVersionDir != null && "snapshot" !in project.version.toString().toLowerCase()) {
        val oldVersionsDir = rootDir.resolve(pluginVersionDir!!)
        println("Using older versions from $oldVersionsDir")
        pluginConfiguration<org.jetbrains.dokka.versioning.VersioningPlugin, org.jetbrains.dokka.versioning.VersioningConfiguration> {
            version = project.version.toString()
            olderVersionsDir = oldVersionsDir
        }
    }
}