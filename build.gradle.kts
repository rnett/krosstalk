plugins {
    kotlin("multiplatform") version "1.4.0" apply false
    kotlin("jvm") version "1.4.0" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.4.0" apply false
    kotlin("kapt") version "1.4.0" apply false
    id("com.github.johnrengelman.shadow") version "5.2.0" apply false
    id("com.jfrog.bintray") version "1.8.5" apply false
    id("com.gradle.plugin-publish") version "0.11.0" apply false
    id("org.jetbrains.dokka") version "1.4.0" apply false
    id("com.github.gmazzo.buildconfig") version "2.0.2" apply false
}

allprojects {
    group = "com.rnett.krosstalk"
    version = "1.0.1-ALPHA"
}

val jvmProjects = setOf("krosstalk-compiler-plugin", "krosstalk-ktor-server")
val multiplatformProjects = setOf("krosstalk", "krosstalk-ktor-client", "krosstalk-ktor-server")

repositories {
    mavenCentral()
    jcenter()
    google()
    mavenLocal()
    maven {
        url = uri("https://dl.bintray.com/kotlin/ktor")
    }
    maven {
        url = uri("https://dl.bintray.com/kotlin/kotlin-eap")
    }
}

allprojects {
    apply(plugin = "org.jetbrains.dokka")

    repositories {
        mavenCentral()
        jcenter()
        google()
        mavenLocal()
        maven("https://dl.bintray.com/kotlin/ktor")
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.useIR = true
    }
}

inline fun Project.jvmProjects(block: () -> Unit) {
    if (this.name in jvmProjects)
        block()
}

inline fun Project.multiplatformProjects(block: () -> Unit) {
    if (this.name in multiplatformProjects)
        block()
}

inline fun nonSampleProjects(crossinline block: Project.() -> Unit) {
    subprojects {
        if (name != "sample")
            block()
    }
}

nonSampleProjects {
    val currentProject = this@nonSampleProjects


    tasks.register("dokkaJar", Jar::class) {
        group = "documentation"
        description = "Assembles Kotlin docs with Dokka"

        archiveClassifier.set("javadoc")
        from(tasks["dokkaHtml"])
        dependsOn(tasks["dokkaHtml"])
    }

    afterEvaluate {
        if (this.name != "krosstalk-gradle-plugin") {
            apply(plugin = "maven-publish")
            apply(plugin = "com.jfrog.bintray")
            apply(plugin = "signing")

            jvmProjects {

                tasks.register<Jar>("sourcesJar") {
                    group = "build"
                    description = "Assembles Kotlin sources"

                    archiveClassifier.set("sources")
                    from((currentProject.properties.getValue("sourceSets") as SourceSetContainer)["main"].allSource)
                    dependsOn(tasks["classes"])
                }

                configure<PublishingExtension> {
                    publications {
                        create<MavenPublication>("default") {
                            from(components["java"])
                            artifact(tasks["sourcesJar"])
                        }
                    }
                }
            }

            configure<org.gradle.plugins.signing.SigningExtension> {
                setRequired(provider { gradle.taskGraph.hasTask("publish") })
                sign(extensions.getByName<PublishingExtension>("publishing").publications)
            }

            configure<PublishingExtension> {
                publications.all {
                    this as MavenPublication
//                    if(name == "krosstalk-compiler-plugin")
//                        from(components["java"])
//                    else
//                        from(components["kotlin"])

//                    artifact(tasks["kotlinSourcesJar"])
                    artifact(tasks["dokkaJar"])

                    pom {
                        name.set(project.name)
                        description.set(currentProject.description)
                        url.set("https://github.com/rnett/krosstalk")

                        licenses {
                            license {
                                name.set("Apache License 2.0")
                                url.set("https://github.com/rnett/krosstalk/blob/master/LICENSE.txt")
                            }
                        }
                        scm {
                            url.set("https://github.com/rnett/krosstalk")
                            connection.set("scm:git:git://github.com/rnett/krosstalk.git")
                        }
                        developers {
                            developer {
                                name.set("Ryan Nett")
                                url.set("https://github.com/rnett")
                            }
                        }
                    }
                }
            }

            configure<com.jfrog.bintray.gradle.BintrayExtension> {
                user = System.getenv("BINTRAY_USER")
                key = System.getenv("BINTRAY_KEY")
                publish = true
                override = true

                setPublications(
                    *currentProject.extensions.getByName<PublishingExtension>("publishing")
                        .publications.map { it.name }.toTypedArray()
                )

                pkg.apply {
                    repo = "krosstalk"
                    name = currentProject.name
                    userOrg = "rnett"
                    githubRepo = githubRepo
                    vcsUrl = "https://github.com/rnett/krosstalk.git"
                    description = currentProject.description
                    setLabels("kotlin", "multiplatform", "js", "server", "api", "compiler")
                    setLicenses("Apache-2.0")
                    desc = description
                    websiteUrl = "https://github.com/rnett/krosstalk"
                    issueTrackerUrl = "https://github.com/rnett/krosstalk/issues"
//                githubReleaseNotesFile = githubReadme

                    version.apply {
                        name = currentProject.version.toString()
                        desc = currentProject.description
                        released = java.util.Date().toString()
                        vcsTag = currentProject.version.toString()
                    }
                }
            }
        }
    }
}

tasks.create("upload") {
    dependsOn("bintrayUpload")
    dependsOn(":krosstalk-gradle-plugin:publishPlugins")
}
