import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.mapProperty
import java.io.File

abstract class PreprocessDocsTask : DefaultTask() {
    @InputFile
    val file = project.objects.fileProperty()

    @Input
    @Optional
    val additionalProperties = project.objects.mapProperty<String, String>()
        .convention(emptyMap())

    @OutputFile
    val outputFile = project.objects.fileProperty()
        .convention {
            val name = file.asFile.get().name
            project.buildDir.resolve("preprocessedDocs").resolve(name)
        }

    private fun processText(text: String, properties: Map<String, String>): String {
        var result = text
        properties.forEach { (key, value) ->
            result = result.replace("$$key", value)
        }
        return result
            .replace("\\$", "$")
            .replace(Regex("]\\(./([^)]*)\\)")) {
                "](" + project.githubRoot + it.groupValues[1] + ")"
            }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun defaultProperties(): Map<String, String> = buildMap {
        put("VERSION", project.version.toString())
        put("GROUP", project.group.toString())
        put("ARTIFACT", project.name)
        put("GIT_BRANCH", project.gitBranch)
        put("GITHUB_ROOT", project.githubRoot)
        val extras = project.extra.properties[extraDocPropertiesName]
        if (extras is Map<*, *>) {
            extras.forEach { (k, v) ->
                put(k.toString(), v.toString())
            }
        }
    }

    @TaskAction
    fun preprocess() {
        val text = file.asFile.orNull?.readText() ?: return
        val properties = defaultProperties() + additionalProperties.get()
        val processed = processText(text, properties)
        val outputFile = outputFile.asFile.get()
        outputFile.writeText(processed)
    }

}

fun Project.preprocessDocs(file: File, additionalProperties: Map<String, String> = emptyMap()): PreprocessDocsTask {
    val outputFile = buildDir.resolve("preprocessedDocs").resolve(file.name)
    return tasks.create("preprocess${file.nameWithoutExtension}", PreprocessDocsTask::class.java) {
        group = "documentation"
        this.file.set(file)
        this.additionalProperties.set(additionalProperties)
        this.outputFile.set(outputFile)
    }
}

fun Project.preprocessDocs(file: String, vararg additionalProperties: Pair<String, String>): PreprocessDocsTask =
    preprocessDocs(project.file(file), additionalProperties.toMap())
