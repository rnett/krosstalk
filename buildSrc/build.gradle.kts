plugins {
    `kotlin-dsl`
    id("com.github.gmazzo.buildconfig") version "3.0.2"
}

repositories {
    mavenCentral()
}

val kotlinVersion = "1.5.21"

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
}

buildConfig{
    packageName("")
    buildConfigField("String", "KOTLIN_VERSION",  "\"$kotlinVersion\"")
}