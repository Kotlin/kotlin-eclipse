package org.jetbrains.kotlin.core.model

import org.eclipse.core.runtime.CoreException
import org.eclipse.core.runtime.IConfigurationElement
import org.eclipse.core.runtime.Platform
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.script.KotlinScriptDefinitionFromTemplate
import org.jetbrains.kotlin.script.ScriptTemplateProvider
import org.jetbrains.kotlin.script.makeScriptDefsFromTemplateProviders
import org.eclipse.core.resources.IFile
import org.jetbrains.kotlin.script.ScriptDependenciesResolver
import org.jetbrains.kotlin.script.ScriptDependenciesResolverEx
import java.net.URLClassLoader
import java.io.File

const val SCRIPT_TEMPLATE_PROVIDER_EP_ID = "org.jetbrains.kotlin.core.scriptTemplateProvider"
const val SCRIPT_TEMPLATE_PROVIDER_EP_EX_ID = "org.jetbrains.kotlin.core.scriptTemplateProviderEx"

fun loadAndCreateDefinitionsByTemplateProviders(eclipseFile: IFile): List<KotlinScriptDefinitionFromTemplate> {
	val scriptTemplateProviders = getScriptProvidersExtensions<ScriptTemplateProvider>(SCRIPT_TEMPLATE_PROVIDER_EP_ID)
	val definitionsFromProviders = makeScriptDefsFromTemplateProviders(scriptTemplateProviders) { provider, e ->
        KotlinLogger.logError(
				"Extension (scriptTemplateProvider) with template ${provider.templateClassName} " +
                "could not be initialized", e)
    }
	
	val scriptTemplateProvidersEx = getScriptProvidersExtensions<ScriptTemplateProviderEx>(SCRIPT_TEMPLATE_PROVIDER_EP_EX_ID)
	val definitionsFromProvidersEx = makeScriptDefsFromEclipseTemplatesProviders(eclipseFile, scriptTemplateProvidersEx)
	
	return definitionsFromProviders + definitionsFromProvidersEx
}

fun <T: Any> getScriptProvidersExtensions(extensionID: String): List<T> {
    return Platform
            .getExtensionRegistry()
            .getConfigurationElementsFor(extensionID)
            .map { ScriptProviderExtensionHelper<T>(it) }
			.mapNotNull { it.createProvider() }
}

class ScriptProviderExtensionHelper<T: Any>(val configurationElement: IConfigurationElement) {
    companion object {
        private const val ID = "id"
        private const val NAME = "name"
        private const val CLASS = "class"
    }
    
    val id: String
        get() = configurationElement.getAttribute(ID)
    
    val name: String
        get() = configurationElement.getAttribute(NAME)
    
    fun createProvider(): T? {
        try {
			@Suppress("UNCHECKED_CAST")
            return configurationElement.createExecutableExtension(CLASS) as T
        } catch(e: CoreException) {
            KotlinLogger.logError(e)
            return null
        }
    }
}

interface ScriptTemplateProviderEx {
	val templateClassNames: Iterable<String>
	val dependenciesClasspath: Iterable<String>
	
	@Suppress("DEPRECATION")
    val resolver: ScriptDependenciesResolver? get() = null
	
	fun isApplicable(file: IFile): Boolean
	fun getEnvironment(file: IFile): Map<String, Any?>? = null
}

fun makeScriptDefsFromEclipseTemplatesProviders(eclipseFile: IFile, providers: Iterable<ScriptTemplateProviderEx>): List<KotlinScriptDefinitionFromTemplate> {
	return providers
			.filter { it. isApplicable(eclipseFile) }
			.flatMap { provider ->
				try {
					val loader = URLClassLoader(
							provider.dependenciesClasspath.map { File(it).toURI().toURL() }.toTypedArray(),
							ScriptTemplateProviderEx::class.java.classLoader)

					provider.templateClassNames.map {
						val cl = loader.loadClass(it)
						KotlinScriptDefinitionFromTemplate(cl.kotlin, provider.getEnvironment(eclipseFile))
					}
				} catch (ex: Exception) {
					KotlinLogger.logError(
							"Extension (EclipseScriptTemplateProvider) ${provider.javaClass.name} with templates ${provider.templateClassNames} " +
									"could not be initialized", ex)
					emptyList<KotlinScriptDefinitionFromTemplate>()
				}
			}
}