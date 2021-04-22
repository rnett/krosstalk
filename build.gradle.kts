buildscript {
    dependencies {
        classpath("org.jetbrains.dokka:versioning-plugin:${Dependencies.dokka}")
    }
}

plugins {
    kotlin("multiplatform") version Dependencies.kotlin apply false
    kotlin("jvm") version Dependencies.kotlin apply false
    id("org.jetbrains.kotlin.plugin.serialization") version Dependencies.kotlin apply false
    kotlin("kapt") version Dependencies.kotlin apply false
    id("com.gradle.plugin-publish") version Dependencies.gradlePluginPublish apply false
    id("org.jetbrains.dokka") version Dependencies.dokka apply true
    id("com.github.gmazzo.buildconfig") version Dependencies.buildconfig apply false
    id("com.vanniktech.maven.publish") version Dependencies.publishPlugin apply false
    signing
}

val sourceLinkBranch: String? by project

val versionDir: String? by project

allprojects {
    version = "0.2.5-SNAPSHOT"

    group = "com.github.rnett.krosstalk"

    repositories {
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
    }

    val isRoot = this == rootProject
    val willPublish = parent != rootProject || isRoot

    if (willPublish) {
        apply(plugin = "org.jetbrains.dokka")
        afterEvaluate {
            apply(plugin = "com.vanniktech.maven.publish")
        }
    }

    afterEvaluate {
        val project = this
        if (this.parent?.name != "plugins") {
            try {
                extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension> {
                    explicitApi()
                }
            } catch (e: UnknownDomainObjectException) {

            }
        }

        if (willPublish) {
            tasks.withType<org.jetbrains.dokka.gradle.AbstractDokkaTask>() {

                val (moduleName, moduleVersion, dokkaSourceSets) = when (this) {
                    is org.jetbrains.dokka.gradle.DokkaTask -> Triple(moduleName, moduleVersion, dokkaSourceSets)
                    is org.jetbrains.dokka.gradle.DokkaTaskPartial -> Triple(moduleName, moduleVersion, dokkaSourceSets)
                    else -> return@withType
                }

                moduleName.set(dokkaModuleName)
                moduleVersion.set(if (sourceLinkBranch == null || sourceLinkBranch == "main") "main" else version.toString())

                dokkaSourceSets.configureEach {
                    if (!isRoot) {
                        includes.from("README.md")
                    }
                    includeNonPublic.set(false)
                    suppressObviousFunctions.set(true)
                    suppressInheritedMembers.set(true)
                    skipDeprecated.set(true)
                    skipEmptyPackages.set(true)
                    jdkVersion.set(8)

                    println("Project: ${project.path}, SourceSet: ${this.sourceSetID}, Platform: ${platform.orNull}")

                    val sourceSet = this.sourceSetID.sourceSetName

                    sourceLink {
                        localDirectory.set(file("src/$sourceSet/kotlin"))

                        remoteUrl.set(java.net.URL(buildString {
                            append("https://github.com/rnett/krosstalk/blob/")
                            append(sourceLinkBranch ?: "main")

                            val dir = projectDir.relativeTo(rootProject.projectDir).path.trim('/')

                            append("/$dir/src/$sourceSet/kotlin")
                        }))
                        remoteLineSuffix.set("#L")
                    }
                }
            }
        }
    }
}

tasks.withType<org.jetbrains.dokka.gradle.DokkaMultiModuleTask>().configureEach {
    removeChildTasks(project(":plugins:krosstalk-compiler-plugin"))
    removeChildTasks(project(":plugins:krosstalk-gradle-plugin"))
    this.fileLayout.set(org.jetbrains.dokka.gradle.DokkaMultiModuleFileLayout.CompactInParent)
    this.includes.from("DOCS.md")
    this.moduleName.set("Krosstalk")
    this.moduleVersion.set(version.toString())

    if (versionDir != null && "snapshot" !in project.version.toString().toLowerCase()) {
        val oldVersionsDir = projectDir.resolve(versionDir!!)
        println("Using older versions from $oldVersionsDir")
        pluginConfiguration<org.jetbrains.dokka.versioning.VersioningPlugin, org.jetbrains.dokka.versioning.VersioningConfiguration> {
            version = project.version.toString()
            olderVersionsDir = oldVersionsDir
        }
    }
}