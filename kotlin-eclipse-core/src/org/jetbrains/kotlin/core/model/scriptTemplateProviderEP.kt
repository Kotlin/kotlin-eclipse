package org.jetbrains.kotlin.core.model

import org.eclipse.core.resources.IFile
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.script.KotlinScriptDefinitionFromAnnotatedTemplate
import org.jetbrains.kotlin.script.ScriptDependenciesResolver
import org.jetbrains.kotlin.script.ScriptTemplatesProvider
import org.jetbrains.kotlin.script.makeScriptDefsFromTemplatesProviders
import java.io.File
import java.net.URLClassLoader

const val SCRIPT_TEMPLATE_PROVIDER_EP_ID = "org.jetbrains.kotlin.core.scriptTemplateProvider"
const val SCRIPT_TEMPLATE_PROVIDER_EP_EX_ID = "org.jetbrains.kotlin.core.scriptTemplateProviderEx"

fun loadAndCreateDefinitionsByTemplateProviders(eclipseFile: IFile): List<KotlinScriptDefinition> {
    val scriptTemplateProviders = loadExecutableEP<ScriptTemplatesProvider>(SCRIPT_TEMPLATE_PROVIDER_EP_ID).mapNotNull { it.createProvider() }
    val definitionsFromProviders = makeScriptDefsFromTemplatesProviders(scriptTemplateProviders) { provider, e ->
        KotlinLogger.logError(
                "Extension (scriptTemplateProvider) with template ${provider.templateClassNames.joinToString()} " +
                "could not be initialized", e)
    }
    
    val scriptTemplateProvidersEx = loadExecutableEP<ScriptTemplateProviderEx>(SCRIPT_TEMPLATE_PROVIDER_EP_EX_ID).mapNotNull { it.createProvider() }
    val definitionsFromProvidersEx = makeScriptDefsFromEclipseTemplatesProviders(eclipseFile, scriptTemplateProvidersEx)
    
    return definitionsFromProviders + definitionsFromProvidersEx
}

interface ScriptTemplateProviderEx {
    val templateClassName: String
    val templateClassClasspath: Iterable<String>

    val resolver: ScriptDependenciesResolver? get() = null

    fun isApplicable(file: IFile): Boolean
    fun getEnvironment(file: IFile): Map<String, Any?>? = null
}

fun makeScriptDefsFromEclipseTemplatesProviders(eclipseFile: IFile, providers: Iterable<ScriptTemplateProviderEx>): List<KotlinScriptDefinition> {
    return providers
            .filter { it.isApplicable(eclipseFile) }
            .map { provider: ScriptTemplateProviderEx ->
                try {
                    val loader = URLClassLoader(
                            provider.templateClassClasspath.map { File(it).toURI().toURL() }.toTypedArray(),
                            ScriptTemplateProviderEx::class.java.classLoader
                    )
                    val cl = loader.loadClass(provider.templateClassName)
                    KotlinScriptDefinitionFromAnnotatedTemplate(cl.kotlin, provider.resolver, null, provider.getEnvironment(eclipseFile))
                } catch (ex: Exception) {
                    KotlinLogger.logError(
                            "Extension (EclipseScriptTemplateProvider) ${provider.javaClass.name} with templates ${provider.templateClassName} " +
                            "could not be initialized", ex)
                    null
                }
            }.filterNotNull()
}