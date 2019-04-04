package org.jetbrains.kotlin.gradle.configurator

import org.eclipse.buildship.core.InitializationContext
import org.eclipse.buildship.core.ProjectConfigurator
import org.eclipse.buildship.core.ProjectContext
import org.eclipse.core.resources.ProjectScope
import org.eclipse.core.runtime.IProgressMonitor
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.core.formatting.KotlinCodeStyleManager
import org.jetbrains.kotlin.core.model.KotlinEnvironment
import org.jetbrains.kotlin.core.model.KotlinNature
import org.jetbrains.kotlin.core.preferences.KotlinCodeStyleProperties
import org.jetbrains.kotlin.core.preferences.KotlinProperties
import org.jetbrains.kotlin.core.utils.ProjectUtils
import org.jetbrains.kotlin.gradle.model.GradleProjectForEclipse
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.gradle.model.GradleMultiProjectForEclipse

class KotlinProjectConfigurator : ProjectConfigurator {

    lateinit var multiModel: GradleMultiProjectForEclipse

    override fun init(context: InitializationContext, monitor: IProgressMonitor) {
        context.gradleBuild.withConnection({
            multiModel = it.getModel(GradleMultiProjectForEclipse::class.java)
        }, monitor)
    }

    override fun configure(context: ProjectContext, monitor: IProgressMonitor) {
        val project = context.project
        KotlinEnvironment.removeEnvironment(project)
        
        if (!::multiModel.isInitialized) return
        val model = multiModel[project.name]
        if (model == null || !model.isKotlinProject) {
            // Kotlin nature may left in .project file after editing out kotlin plugin from gradle buildfile.
            // This leads to nasty bugs so we have to unconfigure project.
            unconfigure(context, monitor)
            return
        }

        KotlinNature.addNature(project)
        if (!ProjectUtils.hasKotlinRuntime(project)) {
            ProjectUtils.addKotlinRuntime(project)
        }

        val compilerProperties = KotlinEnvironment.getEnvironment(project).projectCompilerProperties
        compilerProperties.loadDefaults()
        var configurationChanged = false

        fun String?.configure(action: KotlinProperties.(String) -> Unit) {
            this?.let {
                compilerProperties.action(it)
                configurationChanged = true
            }
        }

        model.jvmTarget.configure { jvmTarget = JvmTarget.fromString(it) ?: JvmTarget.DEFAULT }
        model.languageVersion.configure {
            languageVersion = LanguageVersion.fromVersionString(it) ?: LanguageVersion.LATEST_STABLE
        }
        model.apiVersion.configure {
            apiVersion = ApiVersion.parse(it) ?: ApiVersion.createByLanguageVersion(languageVersion)
        }

        configurationChanged = configurationChanged || model.compilerPlugins.any()
        model.compilerPlugins.forEach {
            compilerProperties.compilerPlugins[it.pluginName].apply {
                active = true
                args += it.options
            }
        }

        if (configurationChanged) {
            compilerProperties.globalsOverridden = true
            compilerProperties.saveChanges()
        }

        model.codestyle
            ?.let { KotlinCodeStyleManager.buildsystemAliases[it] }
            ?.also {
                with(KotlinCodeStyleProperties(ProjectScope(project))) {
                    codeStyleId = it
                    globalsOverridden = true
                    saveChanges()
                }
            }
    }

    override fun unconfigure(context: ProjectContext, monitor: IProgressMonitor) {
        KotlinNature.removeNature(context.project)
    }
}