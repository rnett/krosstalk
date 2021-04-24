import org.gradle.api.Project
import org.gradle.kotlin.dsl.extra

private const val moduleNameProperty = "moduleName"

private fun defaultModuleName(projectName: String): String =
    projectName.replace(Regex("-(.)", RegexOption.DOT_MATCHES_ALL)) {
        " " + it.groupValues[1].toUpperCase()
    }.capitalize()

val Project.dokkaModuleName get() = defaultModuleName(this.name)

val Project.gitBranch
    get() = if (hasProperty("gitBranch"))
        property("gitBranch")?.toString() ?: "main"
    else
        "main"

const val extraDocPropertiesName = "extraDocProperties"

fun Project.docProperty(property: String, value: Any?){
    if(extra.has(extraDocPropertiesName)){
        val map = extra.get(extraDocPropertiesName) as MutableMap<Any?, Any?>
        map[property] = value.toString()
    } else {
        extra.set(extraDocPropertiesName, mutableMapOf(property to value))
    }
}

val Project.githubRoot get() = buildString {
    append("https://github.com/rnett/krosstalk/blob/")
    append(gitBranch)

    val dir = projectDir.relativeTo(rootProject.projectDir).path.trim('/')

    append("/$dir/")
}