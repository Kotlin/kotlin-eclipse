import javax.inject.Inject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.tooling.provider.model.ToolingModelBuilder
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.gradle.model.GradleProjectForEclipse
import org.jetbrains.kotlin.gradle.model.KotlinGradleProject
import org.jetbrains.kotlin.gradle.model.GradleMultiProjectForEclipse
import org.jetbrains.kotlin.gradle.model.GradleMultiProjectForEclipseImpl
import org.jetbrains.kotlin.gradle.model.NonKotlinProject
import org.jetbrains.kotlin.gradle.model.CompilerPluginConfig
import org.jetbrains.kotlin.gradle.model.AllOpen
import org.jetbrains.kotlin.gradle.model.NoArg
import org.jetbrains.kotlin.gradle.model.SAMWithReceiver

initscript {
    dependencies {
        classpath(files(System.getProperty("org.jetbrains.kotlin.eclipse.gradle.model.path")))
    }
}

allprojects {
    buildscript {
        dependencies {
            classpath(files(System.getProperty("org.jetbrains.kotlin.eclipse.gradle.model.path")))
        }
    }

    apply<GradleProjectForEclipseInstaller>()
}

class GradleProjectForEclipseInstaller @Inject constructor(val registry: ToolingModelBuilderRegistry) :
    Plugin<Project> {
    override fun apply(project: Project) {
        registry.register(GradleProjectForEclipseBuilder())
    }
}

class GradleProjectForEclipseBuilder() : ToolingModelBuilder {

    override fun canBuild(modelName: String) = (modelName == GradleMultiProjectForEclipse::class.qualifiedName)

    override fun buildAll(modelName: String, project: Project): GradleMultiProjectForEclipse =
        GradleMultiProjectForEclipseImpl(process(project).toMap())

    private fun process(project: Project): List<Pair<String, GradleProjectForEclipse>> =
        project.childProjects.values.flatMap(::process) +
                (project.name to buildForSubproject(project))

    private fun buildForSubproject(project: Project): GradleProjectForEclipse =
        project.tasks.findByName("compileKotlin")
            ?.dynamicCall("kotlinOptions")
            ?.run {
                KotlinGradleProject(
                    project.findProperty("kotlin.code.style") as? String,
                    property("apiVersion"),
                    property("languageVersion"),
                    property("jvmTarget"),
                    collectPlugins(project)
                )
            } ?: NonKotlinProject

    private fun collectPlugins(project: Project): List<CompilerPluginConfig> {
        val result = arrayListOf<CompilerPluginConfig>()

        project.extensions.findByName("allOpen")?.let {
            AllOpen(
                it.dynamicCall("myAnnotations") as List<String>,
                it.dynamicCall("myPresets") as List<String>
            )
        }?.also { result += it }

        project.extensions.findByName("noArg")?.let {
            NoArg(
                it.dynamicCall("myAnnotations") as List<String>,
                it.dynamicCall("myPresets") as List<String>,
                it.dynamicCall("invokeInitializers") as Boolean
            )
        }?.also { result += it }

        project.extensions.findByName("samWithReceiver")?.let {
            SAMWithReceiver(
                it.dynamicCall("myAnnotations") as List<String>,
                it.dynamicCall("myPresets") as List<String>
            )
        }?.also { result += it }

        return result
    }

    // We need this method, because there is no way for us to get here classes that are added to classpath alongside
// the kotlin gradle plugin. Even if we add them to the classpath of this initscript, they will have different
// classloader.
    fun Any.dynamicCall(name: String, vararg args: Any?): Any? =
        this::class.members.first { it.name == name && it.parameters.size == args.size + 1 }
            .call(this, *args)

    fun Any.property(name: String): String? = dynamicCall(name) as? String
}