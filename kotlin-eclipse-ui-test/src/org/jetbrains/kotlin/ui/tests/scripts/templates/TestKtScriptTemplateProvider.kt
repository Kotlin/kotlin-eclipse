package org.jetbrains.kotlin.ui.tests.scripts.templates

import org.jetbrains.kotlin.script.ScriptTemplateProvider

class TestKtScriptTemplateProvider : ScriptTemplateProvider {
    override val dependenciesClasspath: Iterable<String>
        get() = listOf("bin/")
    
    override val environment: Map<String, Any?>?
        get() = emptyMap()
    
    override val id: String
        get() = "Test"
    
    override val isValid: Boolean
        get() = true
    
    override val templateClassName: String
        get() = "org.jetbrains.kotlin.ui.tests.scripts.templates.TestScriptTemplateDefinition"
    
    override val version: Int
        get() = 10
}