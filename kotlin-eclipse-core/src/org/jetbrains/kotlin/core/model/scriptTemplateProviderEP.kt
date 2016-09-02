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
    val providers = loadExecutableEP<ScriptTemplateProvider>(SCRIPT_TEMPLATE_PROVIDER_EP_ID).mapNotNull { it.createProvider() }
    return makeScriptDefsFromTemplateProviders(providers) { provider, e ->
        KotlinLogger.logError("Extension (scriptTemplateProvider) with template ${provider.templateClassName} " +
                "could not be initialized", e)
    }
}