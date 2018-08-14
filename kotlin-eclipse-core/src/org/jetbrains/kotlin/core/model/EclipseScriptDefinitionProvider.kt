package org.jetbrains.kotlin.core.model

import org.jetbrains.kotlin.core.script.ScriptTemplateContribution
import org.jetbrains.kotlin.core.script.template.ProjectScriptTemplate
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.script.KotlinScriptDefinitionFromAnnotatedTemplate
import org.jetbrains.kotlin.script.ScriptDefinitionProvider
import java.io.File

private const val EXTENSION_POINT_ID = "org.jetbrains.kotlin.core.scriptTemplateContribution"

class EclipseScriptDefinitionProvider: ScriptDefinitionProvider {

    override fun getDefaultScriptDefinition() =
        scriptDefinitions.first { it.template == ProjectScriptTemplate::class }

    override fun getKnownFilenameExtensions(): Sequence<String> =
        scriptDefinitions.map { it.fileExtension }

    override fun findScriptDefinition(fileName: String) =
        scriptDefinitions.firstOrNull { it.isScript(fileName) }

    override fun isScript(fileName: String) =
        scriptDefinitions.any { it.isScript(fileName) }

    companion object {
        private val contributions: List<WrappedContribution> by lazy {
            loadExecutableEP<ScriptTemplateContribution>(EXTENSION_POINT_ID)
                .mapNotNull { it.createProvider() }
                .sortedByDescending { it.priority }
                .map(::WrappedContribution)
        }

        private val scriptDefinitions: Sequence<KotlinScriptDefinition>
            get() = contributions.asSequence().map { it.definition }

        fun getEnvironment(scriptFile: File) =
            contributions.find { it.definition.isScript(scriptFile.name) }
                ?.contribution?.scriptEnvironment(scriptFile)
                ?: emptyMap()
    }
}

private class WrappedContribution(val contribution: ScriptTemplateContribution) {
    val definition by lazy { KotlinScriptDefinitionFromAnnotatedTemplate(template = contribution.template) }
}