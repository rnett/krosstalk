plugins {
    id(libs.plugins.kotlin.jvm.get().pluginId)
    id(libs.plugins.kapt.get().pluginId)
    alias(libs.plugins.compiler.plugin.utils)
    alias(libs.plugins.shadow)
}

description = "Krosstalk Kotlin native compiler plugin"

dependencies {
    implementation(project(":core:krosstalk-base"))

    compileOnly(libs.kotlin.compiler)

    implementation(libs.compiler.plugin.utils.native)

    compileOnly(libs.autoservice.annotations)
    kapt(libs.autoservice)
}

kotlin.irAndJava8()

kotlin {
    this.target {
        mavenPublication {
            project.shadow.component(this)
        }
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
val syncSource = tasks.register<Sync>("syncSource") {
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

tasks.withType<org.jetbrains.dokka.gradle.AbstractDokkaLeafTask>{
    dependsOn(syncSource)
}