plugins {
    `java-gradle-plugin` apply true
    kotlin("jvm")
    kotlin("kapt")
    `maven-publish` apply true
}

group = "com.rnett.krosstalk"
version = "1.0-SNAPSHOT"

repositories {
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
    mavenCentral()
    maven("https://oss.jfrog.org/artifactory/oss-snapshot-local/")
    google()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:1.4.10")

    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.10")

    compileOnly("com.google.auto.service:auto-service-annotations:1.0-rc6")
    kapt("com.google.auto.service:auto-service:1.0-rc6")
}

gradlePlugin {
    plugins {
        create("krosstalkPlugin") {
            id = "com.rnett.krosstalk-gradle-plugin"
            implementationClass = "com.rnett.krosstalk.KrosstalkGradlePlugin"
        }
    }
}

tasks.getByName("compileKotlin") {
    dependsOn(":krosstalk-compiler-plugin:publishToMavenLocal")
}