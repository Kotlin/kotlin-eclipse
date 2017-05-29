package org.jetbrains.kotlin.core.model

import org.eclipse.core.resources.IFile
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.SubMonitor
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.script.KotlinScriptDefinitionFromAnnotatedTemplate
import org.jetbrains.kotlin.script.ScriptTemplatesProvider
import org.jetbrains.kotlin.script.makeScriptDefsFromTemplatesProviders
import java.io.File
import java.net.URLClassLoader

const val SCRIPT_TEMPLATE_PROVIDER_EP_ID = "org.jetbrains.kotlin.core.scriptTemplateProvider"
const val SCRIPT_TEMPLATE_PROVIDER_EP_EX_ID = "org.jetbrains.kotlin.core.scriptTemplateProviderEx"

fun loadAndCreateDefinitionsByTemplateProviders(
        eclipseFile: IFile,
        monitor: IProgressMonitor
): Pair<List<KotlinScriptDefinition>, List<String>> {
    val scriptTemplateProviders = loadExecutableEP<ScriptTemplatesProvider>(SCRIPT_TEMPLATE_PROVIDER_EP_ID).mapNotNull { it.createProvider() }
    val definitionsFromProviders = makeScriptDefsFromTemplatesProviders(scriptTemplateProviders) { provider, e ->
        KotlinLogger.logError(
                "Extension (scriptTemplateProvider) with template ${provider.templateClassNames.joinToString()} " +
                "could not be initialized", e)
    }
    
    val scriptTemplateProvidersEx = loadExecutableEP<ScriptTemplateProviderEx>(SCRIPT_TEMPLATE_PROVIDER_EP_EX_ID).mapNotNull { it.createProvider() }
    val definitionsFromProvidersEx = makeScriptDefsFromEclipseTemplatesProviders(eclipseFile, scriptTemplateProvidersEx, monitor)
    val onlyProvidersEx = definitionsFromProvidersEx.map { it.first }
    val providersClasspath = definitionsFromProvidersEx.flatMap { it.second }
        
    return Pair(definitionsFromProviders + onlyProvidersEx, providersClasspath)
}

interface ScriptTemplateProviderEx {
    val templateClassName: String
    
    fun isApplicable(file: IFile): Boolean = true

    fun getTemplateClasspath(environment: Map<String, Any?>?, monitor: IProgressMonitor): Iterable<String>
    fun getEnvironment(file: IFile): Map<String, Any?>?
}

fun makeScriptDefsFromEclipseTemplatesProviders(
        eclipseFile: IFile,
        providers: Iterable<ScriptTemplateProviderEx>,
        monitor: IProgressMonitor
): List<Pair<KotlinScriptDefinition, Iterable<String>>> {
    return providers
            .filter { it.isApplicable(eclipseFile) }
            .map { provider: ScriptTemplateProviderEx ->
                try {
                    val subMonitor = SubMonitor.convert(monitor)
                    val templateClasspath = provider.getTemplateClasspath(provider.getEnvironment(eclipseFile), subMonitor)
                    
                    if (subMonitor.isCanceled) return@map null
                    
                    val loader = URLClassLoader(
                            templateClasspath.map { File(it).toURI().toURL() }.toTypedArray(),
                            ScriptTemplateProviderEx::class.java.classLoader
                    )
                    
                    for (cp in templateClasspath) {
                    	KotlinLogger.logWarning("Load for ${provider.templateClassName}: $cp")
                    }
                    
                    val cl = loader.loadClass(provider.templateClassName)
                    Pair(KotlinScriptDefinitionFromAnnotatedTemplate(cl.kotlin, null, null, provider.getEnvironment(eclipseFile)), templateClasspath)
                } catch (ex: Exception) {
                    KotlinLogger.logError(
                            "Extension (EclipseScriptTemplateProvider) ${provider::class.java.name} with templates ${provider.templateClassName} " +
                            "could not be initialized", ex)
                    null
                }
            }
            .filterNotNull()
}