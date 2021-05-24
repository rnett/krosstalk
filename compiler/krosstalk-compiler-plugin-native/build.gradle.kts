plugins {
    kotlin("jvm")
    kotlin("kapt")
    id("com.github.rnett.compiler-plugin-utils") version Dependencies.compilerPluginUtils
    id("com.github.johnrengelman.shadow")
}

description = "Krosstalk Kotlin native compiler plugin"

dependencies {
    implementation(project(":core:krosstalk-base"))
    implementation("com.github.rnett.compiler-plugin-utils:compiler-plugin-utils-native:${Dependencies.compilerPluginUtils}")

    compileOnly("org.jetbrains.kotlin:kotlin-compiler:${Dependencies.kotlin}")
    compileOnly("com.google.auto.service:auto-service-annotations:${Dependencies.autoService}")

    kapt("com.google.auto.service:auto-service:${Dependencies.autoService}")
}

kotlin.irAndJava8()

kotlin {
    this.target {
        mavenPublication {
            project.shadow.component(this)
        }
//        components.forEach {
//            it as AdhocComponentWithVariants
//            it.wi
//
//        }
    }
}

val shadowJar = tasks.shadowJar.apply {
    configure {
        archiveClassifier.set("")
        dependencies {
            include(project(":core:krosstalk-base"))
            include(dependency("com.github.rnett.compiler-plugin-utils:compiler-plugin-utils-native"))
        }
    }
}

tasks.jar.configure {
    finalizedBy(shadowJar)
}

tasks.named("compileKotlin") { dependsOn("syncSource") }
tasks.register<Sync>("syncSource") {
    from(project(":compiler:krosstalk-compiler-plugin").sourceSets.main.get().allSource)
    into("src/main/kotlin")
    filter {
        // Replace shadowed imports from plugin module
        when (it) {
            "import org.jetbrains.kotlin.com.intellij.mock.MockProject" -> "import com.intellij.mock.MockProject"
            else -> it
        }
    }
}