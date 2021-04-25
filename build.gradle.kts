plugins {
    kotlin("multiplatform") version Dependencies.kotlin apply false
    kotlin("jvm") version Dependencies.kotlin apply false
    id("org.jetbrains.kotlin.plugin.serialization") version Dependencies.kotlin apply false
    kotlin("kapt") version Dependencies.kotlin apply false
    id("com.gradle.plugin-publish") version Dependencies.gradlePluginPublish apply false
    id("org.jetbrains.dokka") version Dependencies.dokka apply false
    id("com.github.gmazzo.buildconfig") version Dependencies.buildconfig apply false
    id("com.vanniktech.maven.publish") version Dependencies.publishPlugin apply false
    signing
}

allprojects {
    version = "0.2.7"

    group = "com.github.rnett.krosstalk"

    repositories {
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
    }

    val isRoot = this == rootProject
    val generateDocs = parent != rootProject || isRoot
    val willPublish = childProjects.isEmpty()

    if (generateDocs)
        apply(plugin = "org.jetbrains.dokka")

    if (willPublish)
        afterEvaluate { apply(plugin = "com.vanniktech.maven.publish") }

    afterEvaluate {
        val project = this
        if (this.parent?.name != "compiler") {
            try {
                extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension> {
                    explicitApi()
                }
            } catch (e: UnknownDomainObjectException) {

            }
        }

        if (generateDocs) {
            val docs = preprocessDocs("README.md")
            tasks.withType<org.jetbrains.dokka.gradle.AbstractDokkaTask>() {

                val (moduleName, moduleVersion, dokkaSourceSets) = when (this) {
                    is org.jetbrains.dokka.gradle.DokkaTask -> Triple(moduleName, moduleVersion, dokkaSourceSets)
                    is org.jetbrains.dokka.gradle.DokkaTaskPartial -> Triple(moduleName, moduleVersion, dokkaSourceSets)
                    else -> return@withType
                }

                moduleName.set(dokkaModuleName)
                moduleVersion.set(version.toString())

                dokkaSourceSets.configureEach {
                    if (!isRoot && "compiler" !in project.path) {
                        includes.from(docs)
                    }
                    includeNonPublic.set(false)
                    suppressObviousFunctions.set(true)
                    suppressInheritedMembers.set(true)
                    skipDeprecated.set(true)
                    skipEmptyPackages.set(true)
                    jdkVersion.set(8)

                    val sourceSet = this.sourceSetID.sourceSetName

                    sourceLink {
                        localDirectory.set(file("src/$sourceSet/kotlin"))

                        remoteUrl.set(java.net.URL("$githubRoot/src/$sourceSet/kotlin"))
                        remoteLineSuffix.set("#L")
                    }
                }
            }
        }
    }
}