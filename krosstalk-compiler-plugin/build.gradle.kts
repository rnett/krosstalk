plugins {
    kotlin("jvm")
    kotlin("kapt")
    `maven-publish` apply true
    id("com.github.johnrengelman.shadow") apply true
}

group = "com.rnett.krosstalk"
version = "1.0-SNAPSHOT"

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.4.10")
//    implementation("io.arrow-kt:compiler-plugin:1.4-M1-SNAPSHOT")

    compileOnly("com.google.auto.service:auto-service-annotations:1.0-rc6")
    kapt("com.google.auto.service:auto-service:1.0-rc6")
//    implementation(project(":krosstalk"))
}

kotlin {
    target.compilations.all {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.freeCompilerArgs += "-Xjvm-default=enable"
    }
}

tasks.getByName<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    configurations.add(project.configurations.compile.get())

    relocate("org.jetbrains.kotlin.com.intellij", "com.intellij")

    dependencies {
        exclude {
            it.moduleGroup == "org.jetbrains.kotlin"
        }
    }
}

val sourcesJar = tasks.create<Jar>("sourcesJar") {
    classifier = "sources"
    from(kotlin.sourceSets["main"].kotlin.srcDirs)
}

publishing {
    publications {
        create<MavenPublication>("default") {
            from(components["java"])
            artifact(sourcesJar)
        }
    }
}