package org.jetbrains.kotlin.maven.configuration

import org.apache.maven.model.Plugin
import org.apache.maven.project.MavenProject
import org.eclipse.core.resources.ProjectScope
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest
import org.eclipse.m2e.jdt.AbstractSourcesGenerationProjectConfigurator
import org.eclipse.m2e.jdt.IClasspathDescriptor
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.core.KotlinClasspathContainer
import org.jetbrains.kotlin.core.formatting.KotlinCodeStyleManager
import org.jetbrains.kotlin.core.model.KotlinEnvironment
import org.jetbrains.kotlin.core.model.KotlinNature
import org.jetbrains.kotlin.core.preferences.KotlinCodeStyleProperties

private const val GROUP_ID = "org.jetbrains.kotlin"
private const val MAVEN_PLUGIN_ID = "kotlin-maven-plugin"

class KotlinMavenProjectConfigurator : AbstractSourcesGenerationProjectConfigurator() {

    override fun configure(request: ProjectConfigurationRequest, monitor: IProgressMonitor) {
        val mavenProject = request.mavenProject
        val plugin: Plugin = mavenProject.buildPlugins
            .find { checkCoordinates(it, GROUP_ID, MAVEN_PLUGIN_ID) } ?: return

        val compilerProperties = KotlinEnvironment.getEnvironment(request.project).projectCompilerProperties
        var configurationChanged = false

        fun configureProperty(
            attributeName: String? = null,
            propertyPath: String? = null,
            isChangingConfiguration: Boolean = true,
            callback: (String) -> Unit
        ) {
            MavenAttributeAccessor(attributeName, propertyPath)
                .getFrom(mavenProject, plugin)
                ?.also(callback)
                ?.also { configurationChanged = configurationChanged || isChangingConfiguration }
        }


        configureProperty("languageVersion", "kotlin.compiler.languageVersion") {
            compilerProperties.languageVersion = LanguageVersion.fromVersionString(it) ?: LanguageVersion.LATEST_STABLE
        }

        configureProperty("apiVersion", "kotlin.compiler.apiVersion") {
            compilerProperties.apiVersion = ApiVersion.parse(it)
                    ?: ApiVersion.createByLanguageVersion(compilerProperties.languageVersion)
        }

        configureProperty("jvmTarget", "kotlin.compiler.jvmTarget") {
            compilerProperties.jvmTarget = JvmTarget.fromString(it) ?: JvmTarget.DEFAULT
        }

        configureProperty(propertyPath = "kotlin.code.style", isChangingConfiguration = false) { alias ->
            KotlinCodeStyleManager.buildsystemAliases[alias]?.let {
                with (KotlinCodeStyleProperties(ProjectScope(request.project))) {
                    codeStyleId = it
                    globalsOverridden = true
                    saveChanges()
                }
            }
        }

        if (configurationChanged) {
            compilerProperties.globalsOverridden = true
            compilerProperties.saveChanges()
        }
    }

    override fun configureRawClasspath(
        request: ProjectConfigurationRequest, classpath: IClasspathDescriptor,
        monitor: IProgressMonitor
    ) {
        if (hasKotlinMavenPlugin(request.mavenProject)) {
            classpath.addEntry(KotlinClasspathContainer.CONTAINER_ENTRY)
            AbstractProjectConfigurator.addNature(request.project, KotlinNature.KOTLIN_NATURE, monitor)
        }
    }

    private fun hasKotlinMavenPlugin(mavenProject: MavenProject): Boolean =
        mavenProject.buildPlugins.any { checkCoordinates(it, GROUP_ID, MAVEN_PLUGIN_ID) }

    private fun checkCoordinates(buildPlugin: Plugin, groupId: String, artifactId: String): Boolean =
        groupId == buildPlugin.groupId && artifactId == buildPlugin.artifactId
}