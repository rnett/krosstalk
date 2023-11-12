package kbuild

import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

plugins {
    kotlin("multiplatform")
    id("kbuild.kotlin-base")
}

val extension = extensions.create<MultiplatformExtension>(MultiplatformExtension.NAME).apply {
    onlyPublishFromMainHost.convention(setOf("jvm", "js", "kotlinMultiplatform"))
}

val libs = versionCatalogs.named("libs")

kotlin {
    explicitApi()
    targets.withType<KotlinJvmTarget>() {
        //TODO remove once KT-36942 and KT-35003 are fixed
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 8)
        }
        compilations.configureEach {
            kotlinOptions {
                jvmTarget = "1.8"
                //TODO remove once KT-36942 and KT-35003 are fixed
                compileJavaTaskProvider?.configure {
                    targetCompatibility = "1.8"
                    sourceCompatibility = "1.8"
                }
            }
        }
    }

    sourceSets {
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.findLibrary("kotlinx.coroutines.test").orElseThrow())
            }
        }

        configureEach {
            if (name == "jvmTest") {
                dependencies {
                    implementation(kotlin("test-junit5"))
                    implementation(libs.findLibrary("mockk").orElseThrow())
                }
            }
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

extensions.findByType<PublishingExtension>()?.let {
    extensions.configure<PublishingExtension> {
        publications {
            configureEach {
                if (name in extension.onlyPublishFromMainHost.get()) {
                    tasks.withType<AbstractPublishToMaven>()
                        .matching { it.publication == this }
                        .configureEach {
                            onlyIf {
                                MultiplatformExtension.isMainHost
                            }
                        }
                }
            }
        }
    }
}