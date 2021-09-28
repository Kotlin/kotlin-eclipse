package org.jetbrains.kotlin.core.model

import org.eclipse.osgi.internal.loader.EquinoxClassLoader
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.core.script.ScriptTemplateContribution
import org.jetbrains.kotlin.core.script.template.ProjectScriptTemplate
import org.jetbrains.kotlin.core.utils.ProjectUtils
import org.jetbrains.kotlin.core.utils.asResource
import org.jetbrains.kotlin.scripting.definitions.KotlinScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionProvider
import org.jetbrains.kotlin.scripting.resolve.KtFileScriptSource
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.full.hasAnnotation
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.KotlinType
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.configurationDependencies
import kotlin.script.experimental.host.createEvaluationConfigurationFromTemplate
import kotlin.script.experimental.host.getScriptingClass
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.JvmGetScriptingClass

private const val EXTENSION_POINT_ID = "org.jetbrains.kotlin.core.scriptTemplateContribution"

class EclipseScriptDefinitionProvider : ScriptDefinitionProvider {
    override fun getDefaultDefinition() =
            scriptDefinitions.first { it.baseClassType == KotlinType(ProjectScriptTemplate::class) }

    override fun findDefinition(script: SourceCode): ScriptDefinition? =
            scriptDefinitions.first { it.isScript(script) }

    override fun findScriptDefinition(fileName: String): KotlinScriptDefinition? =
            scriptDefinitions.map { it.legacyDefinition }.first { it.isScript(fileName) }

    override fun getDefaultScriptDefinition() =
            getDefaultDefinition().legacyDefinition

    override fun getKnownFilenameExtensions(): Sequence<String> =
            scriptDefinitions.map { it.fileExtension }

    override fun isScript(script: SourceCode) = Companion.isScript(script)

    companion object {
        private val contributions: List<WrappedContribution> by lazy {
            loadExecutableEP<ScriptTemplateContribution>(EXTENSION_POINT_ID)
                    .mapNotNull { it.createProvider() }
                    .sortedByDescending { it.priority }
                    .map(::WrappedContribution)
        }

        private val scriptDefinitions: Sequence<ScriptDefinition>
            get() = contributions.asSequence().map { it.definition }

        fun isScript(script: SourceCode) = scriptDefinitions.any { it.isScript(script) }

        fun getEnvironment(scriptFile: File) =
                scriptFile.asResource
                        ?.let { KotlinPsiManager.getKotlinParsedFile(it) }
                        ?.let { KtFileScriptSource(it) }
                        ?.let { source ->
                            contributions.find { it.definition.isScript(source) }
                                    ?.contribution?.scriptEnvironment(scriptFile)
                                    ?: emptyMap()
                        }
    }
}

private class WrappedContribution(val contribution: ScriptTemplateContribution) {
    val definition: ScriptDefinition by lazy {
        if (contribution.template.hasAnnotation<KotlinScript>()) {
            ScriptDefinition.FromTemplate(
                    baseHostConfiguration = ScriptingHostConfiguration {
                        getScriptingClass(JvmGetScriptingClass())
                        configurationDependencies(JvmDependency(extractClasspath(contribution.template) + scriptingDependencies))
                    },
                    template = contribution.template,
                    contextClass = contribution::class
            )
        } else {
            ScriptDefinition.FromLegacyTemplate(
                    hostConfiguration = ScriptingHostConfiguration {
                        getScriptingClass(JvmGetScriptingClass())
                        configurationDependencies(JvmDependency(extractClasspath(contribution.template) + scriptingDependencies))
                    },
                    template = contribution.template
            )
        }
    }
}

// TODO: hack for now, will definitely need rethinking
private fun extractClasspath(kClass: KClass<*>): List<File> =
        (kClass.java.classLoader as? EquinoxClassLoader)
                ?.classpathManager
                ?.hostClasspathEntries
                ?.map { entry -> entry.bundleFile.baseFile.resolve("bin") }
                .orEmpty()

private val scriptingDependencies: List<File> by lazy {
    listOf("kotlin-scripting-jvm")
            .map { File(ProjectUtils.buildLibPath(it)) }
}