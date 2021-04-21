import org.gradle.api.Project
import org.gradle.kotlin.dsl.extra

private const val moduleNameProperty = "moduleName"

private fun defaultModuleName(projectName: String): String = projectName.replace(Regex("-(.)", RegexOption.DOT_MATCHES_ALL)) {
    " " + it.groupValues[1].toUpperCase()
}.capitalize()

var Project.moduleName: String
    get() = if (extra.has(moduleNameProperty)) extra[moduleNameProperty].toString() else defaultModuleName(project.name)
    set(value) {
        extra[moduleNameProperty] = value
    }