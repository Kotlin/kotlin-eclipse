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

class KotlinProjectConfigurator : ProjectConfigurator {

    lateinit var model: GradleProjectForEclipse

    override fun init(context: InitializationContext, monitor: IProgressMonitor) {
        context.gradleBuild.withConnection({
            model = it.getModel(GradleProjectForEclipse::class.java)
        }, monitor)
    }

    override fun configure(context: ProjectContext, monitor: IProgressMonitor) {
        if (!::model.isInitialized || !model.isKotlinProject) return

        val project = context.project

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