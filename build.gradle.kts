plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false

    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.publish) apply false

    signing
}

allprojects {

    version = "1.3.0"
    group = "com.github.rnett.krosstalk"

    repositories {
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots"){
            mavenContent{ snapshotsOnly() }
        }
        maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
    }

    val isRoot = this == rootProject
    val generateDocs = parent != rootProject || isRoot
    val willPublish = childProjects.isEmpty()

    if (generateDocs)
        apply(plugin = "org.jetbrains.dokka")

    if (willPublish)
        afterEvaluate {
            apply(plugin = "org.gradle.maven-publish")

            val project = this

            apply(plugin = "com.vanniktech.maven.publish")

            extensions.getByType<com.vanniktech.maven.publish.MavenPublishBaseExtension>().apply {
                if (!version.toString().toLowerCase().endsWith("snapshot")) {
                    val stagingProfileId = project.findProperty("sonatypeRepositoryId")?.toString()
                    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.DEFAULT, stagingProfileId)
                }

                pom {
                    name.set(project.niceModuleName)
                    description.set(project.description ?: "Krosstalk module")
                    inceptionYear.set("2021")
                    url.set("https://github.com/rnett/krosstalk/")

                    licenses {
                        license {
                            name.set("The Apache Software License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                            distribution.set("repo")
                        }
                    }

                    scm {
                        url.set("https://github.com/rnett/krosstalk.git")
                        connection.set("scm:git:git://github.com/rnett/krosstalk.git")
                        developerConnection.set("scm:git:ssh://git@github.com/rnett/krosstalk.git")
                    }

                    developers {
                        developer {
                            id.set("rnett")
                            name.set("Ryan Nett")
                            url.set("https://github.com/rnett/")
                        }
                    }
                }
            }
        }

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
            tasks.withType<org.jetbrains.dokka.gradle.AbstractDokkaLeafTask>() {
                moduleName.set(niceModuleName)
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

                    val localDir = file("src/$sourceSet/kotlin")
                    if(localDir.exists()) {
                        sourceLink {
                            localDirectory.set(localDir)

                            remoteUrl.set(java.net.URL("$githubRoot/src/$sourceSet/kotlin"))
                            remoteLineSuffix.set("#L")
                        }
                    }
                }
            }
        }
    }
}

val header = "Krosstalk: A pure Kotlin pluggable RPC library"

tasks.create<Copy>("generateReadme"){
    from("README.md")
    into(buildDir)
    filter{
        it.replace("# $header",
            "# [$header](https://github.com/rnett/krosstalk)")
    }
}
