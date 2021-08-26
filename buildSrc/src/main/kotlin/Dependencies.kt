import org.gradle.kotlin.dsl.DependencyHandlerScope
import BuildConfig

object Dependencies {
    val dokkaVersioning = "${BuildConfig.DOKKA_VERSIONING_MODULE}:${BuildConfig.DOKKA_VERSION}"
}