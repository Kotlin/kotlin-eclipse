import javax.inject.Inject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.tooling.provider.model.ToolingModelBuilder
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.gradle.model.GradleProjectForEclipse
import org.jetbrains.kotlin.gradle.model.GradleProjectForEclipseImpl
import org.jetbrains.kotlin.gradle.model.NoKotlinProject

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

class GradleProjectForEclipseInstaller @Inject constructor(val registry: ToolingModelBuilderRegistry) : Plugin<Project> {
    override fun apply(project: Project) {
        registry.register(GradleProjectForEclipseBuilder())
    }
}

class GradleProjectForEclipseBuilder() : ToolingModelBuilder {
    override fun canBuild(modelName: String) = modelName == GradleProjectForEclipse::class.qualifiedName

    override fun buildAll(modelName: String, project: Project): Any {
        val task = project.tasks["compileKotlin"] 
        
        return task?.dynamicCall("kotlinOptions")?.run {
            GradleProjectForEclipseImpl(
                project.findProperty("kotlin.code.style") as? String,
                property("apiVersion"),
                property("languageVersion"),
                property("jvmTarget")
            )
        } ?: return NoKotlinProject
    }
    
    // We need this method, because there is no way for us to get here classes that are added to classpath alongside
    // the kotlin gradle plugin. Even if we add them to the classpath of this initscript, they will have different
    // classloader.
    fun Any.dynamicCall(name: String, vararg args: Any?): Any? =
        this::class.members.first { it.name == name && it.parameters.size == args.size + 1 }
                .call(this, *args)
    
    fun Any.property(name: String): String? = dynamicCall(name) as? String
}