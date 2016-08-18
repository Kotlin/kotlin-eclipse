package org.jetbrains.kotlin.core.model

import org.eclipse.core.resources.IFile
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.script.KotlinScriptDefinitionFromTemplate
import org.jetbrains.kotlin.script.ScriptDependenciesResolver
import org.jetbrains.kotlin.script.ScriptTemplateProvider
import org.jetbrains.kotlin.script.makeScriptDefsFromTemplateProviders
import java.io.File
import java.net.URLClassLoader

const val SCRIPT_TEMPLATE_PROVIDER_EP_ID = "org.jetbrains.kotlin.core.scriptTemplateProvider"
const val SCRIPT_TEMPLATE_PROVIDER_EP_EX_ID = "org.jetbrains.kotlin.core.scriptTemplateProviderEx"

fun loadAndCreateDefinitionsByTemplateProviders(eclipseFile: IFile): List<KotlinScriptDefinitionFromTemplate> {
    val scriptTemplateProviders = loadExecutableEP<ScriptTemplateProvider>(SCRIPT_TEMPLATE_PROVIDER_EP_ID).mapNotNull { it.createProvider() }
    val definitionsFromProviders = makeScriptDefsFromTemplateProviders(scriptTemplateProviders) { provider, e ->
        KotlinLogger.logError(
                "Extension (scriptTemplateProvider) with template ${provider.templateClassName} " +
                "could not be initialized", e)
    }
    
    val scriptTemplateProvidersEx = loadExecutableEP<ScriptTemplateProviderEx>(SCRIPT_TEMPLATE_PROVIDER_EP_EX_ID).mapNotNull { it.createProvider() }
    val definitionsFromProvidersEx = makeScriptDefsFromEclipseTemplatesProviders(eclipseFile, scriptTemplateProvidersEx)
    
    return definitionsFromProviders + definitionsFromProvidersEx
}

interface ScriptTemplateProviderEx {
    val templateClassNames: Iterable<String>
    val dependenciesClasspath: Iterable<String>

    val resolver: ScriptDependenciesResolver? get() = null

    fun isApplicable(file: IFile): Boolean
    fun getEnvironment(file: IFile): Map<String, Any?>? = null
}

fun makeScriptDefsFromEclipseTemplatesProviders(eclipseFile: IFile, providers: Iterable<ScriptTemplateProviderEx>): List<KotlinScriptDefinitionFromTemplate> {
    return providers
            .filter { it.isApplicable(eclipseFile) }
            .flatMap { provider ->
                try {
                    val loader = URLClassLoader(
                            provider.dependenciesClasspath.map { File(it).toURI().toURL() }.toTypedArray(),
                            ScriptTemplateProviderEx::class.java.classLoader)

                    provider.templateClassNames.map {
                        val cl = loader.loadClass(it)
                        KotlinScriptDefinitionFromTemplate(cl.kotlin, provider.resolver, null, provider.getEnvironment(eclipseFile))
                    }
                } catch (ex: Exception) {
                    KotlinLogger.logError(
                            "Extension (EclipseScriptTemplateProvider) ${provider.javaClass.name} with templates ${provider.templateClassNames} " +
                                    "could not be initialized", ex)
                    emptyList<KotlinScriptDefinitionFromTemplate>()
                }
            }
}