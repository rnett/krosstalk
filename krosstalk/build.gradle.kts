plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
    application
    `maven-publish` apply true

}
group = "com.rnett.krosstalk"
version = "1.0-SNAPSHOT"

val coroutines_version = "1.3.9"
val serialization_version = "1.0.0-RC"

repositories {
    mavenCentral()
    jcenter()
    maven {
        url = uri("https://dl.bintray.com/kotlin/kotlin-eap")
    }
}
kotlin {
    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
                useIR = true
            }
        }
        withJava()
    }
    js(BOTH) {
//        produceExecutable()

        browser { }


        configure(compilations) {
            kotlinOptions {
                noStdlib = true
                sourceMapEmbedSources = "always"
                metaInfo = true
                sourceMap = true
                moduleKind = "commonjs"
            }
        }
    }

    configure(listOf(targets["metadata"], jvm(), js())) {
        mavenPublication {
            val targetPublication = this@mavenPublication
            tasks.withType<AbstractPublishToMaven>()
                .matching { it.publication == targetPublication }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
//                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serialization_version")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:$serialization_version")
            }
        }

        val jsMain by getting {

        }
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("reflect"))
            }
        }

        all {
            languageSettings.apply {
                enableLanguageFeature("InlineClasses")
                enableLanguageFeature("NewInference")
                useExperimentalAnnotation("kotlin.contracts.ExperimentalContracts")
                useExperimentalAnnotation("kotlin.RequiresOptIn")
                useExperimentalAnnotation("kotlin.time.ExperimentalTime")
            }
        }
    }
}

//val sourcesJar = tasks.create<Jar>("sourcesJar") {
//    classifier = "sources"
//    from(kotlin.sourceSets["main"].kotlin.srcDirs)
//}

publishing {
//    publications {
//        create<MavenPublication>("default") {
//            from(components["java"])
//            artifact(sourcesJar)
//        }
//    }
}