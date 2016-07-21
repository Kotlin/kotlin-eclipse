package org.jetbrains.kotlin.core.model

import org.eclipse.core.runtime.IConfigurationElement
import org.eclipse.core.runtime.Platform
import org.jetbrains.kotlin.core.Activator
import org.jetbrains.kotlin.script.KotlinScriptDefinitionFromTemplate
import org.jetbrains.kotlin.script.ScriptTemplateProvider
import org.jetbrains.kotlin.script.makeScriptDefsFromTemplateProviders

const val SCRIPT_TEMPLATE_PROVIDER_EP_ID = "org.jetbrains.kotlin.core.scriptTemplateProvider"

fun loadAndCreateDefinitionsByTemplateProviders(): List<KotlinScriptDefinitionFromTemplate> {
    val providers = loadScriptTemplateProviders().map { it.createProvider() }
    return makeScriptDefsFromTemplateProviders(providers)
}

fun loadScriptTemplateProviders(): List<ScriptTemplateProviderDescriptor> {
    return Platform
            .getExtensionRegistry()
            .getConfigurationElementsFor(Activator.PLUGIN_ID, SCRIPT_TEMPLATE_PROVIDER_EP_ID)
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
    
    fun createProvider(): ScriptTemplateProvider {
        return configurationElement.createExecutableExtension(CLASS) as ScriptTemplateProvider
    }
}