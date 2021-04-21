import org.gradle.api.Project

private const val moduleNameProperty = "moduleName"

private fun defaultModuleName(projectName: String): String = projectName.replace(Regex("-(.)", RegexOption.DOT_MATCHES_ALL)) {
    " " + it.groupValues[1].toUpperCase()
}.capitalize()

val Project.dokkaModuleName get() = defaultModuleName(this.name)