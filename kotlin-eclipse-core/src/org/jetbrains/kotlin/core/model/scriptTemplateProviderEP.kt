package org.jetbrains.kotlin.core.model

import org.eclipse.core.runtime.CoreException
import org.eclipse.core.runtime.IConfigurationElement
import org.eclipse.core.runtime.Platform
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.script.KotlinScriptDefinitionFromTemplate
import org.jetbrains.kotlin.script.ScriptTemplateProvider
import org.jetbrains.kotlin.script.makeScriptDefsFromTemplateProviders

const val SCRIPT_TEMPLATE_PROVIDER_EP_ID = "org.jetbrains.kotlin.core.scriptTemplateProvider"

fun loadAndCreateDefinitionsByTemplateProviders(): List<KotlinScriptDefinitionFromTemplate> {
    val providers = loadScriptTemplateProviders().mapNotNull { it.createProvider() }
    return makeScriptDefsFromTemplateProviders(providers) { provider, e ->
        KotlinLogger.logError("Extension (scriptTemplateProvider) with template ${provider.templateClassName} " +
                "could not be initialized", e)
    }
}

fun loadScriptTemplateProviders(): List<ScriptTemplateProviderDescriptor> {
    return Platform
            .getExtensionRegistry()
            .getConfigurationElementsFor(SCRIPT_TEMPLATE_PROVIDER_EP_ID)
            .map(::ScriptTemplateProviderDescriptor)
}

class ScriptTemplateProviderDescriptor(val configurationElement: IConfigurationElement) {
    companion object {
        private const val ID = "id"
        private const val NAME = "name"
        private const val CLASS = "class"
    }
    
    val id: String
        get() = configurationElement.getAttribute(ID)
    
    val name: String
        get() = configurationElement.getAttribute(NAME)
    
    fun createProvider(): ScriptTemplateProvider? {
        try {
            return configurationElement.createExecutableExtension(CLASS) as ScriptTemplateProvider
        } catch(e: CoreException) {
            KotlinLogger.logError(e)
            return null
        }
    }
}