plugins {
    kotlin("jvm")
    kotlin("kapt")
    id("com.github.rnett.compiler-plugin-utils") version Dependencies.compilerPluginUtils
}

description = "Krosstalk Kotlin native compiler plugin"

dependencies {
    implementation(kotlin("reflect"))
    implementation(project(":core:krosstalk-base"))
    compileOnly("org.jetbrains.kotlin:kotlin-compiler:${Dependencies.kotlin}")

    implementation("com.github.rnett.compiler-plugin-utils:compiler-plugin-utils:${Dependencies.compilerPluginUtils}")

    compileOnly("com.google.auto.service:auto-service-annotations:${Dependencies.autoService}")
    kapt("com.google.auto.service:auto-service:${Dependencies.autoService}")
}

kotlin.irAndJava8(project)

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