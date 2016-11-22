package org.jetbrains.kotlin.ui.tests.scripts.templates

import org.jetbrains.kotlin.script.ScriptTemplatesProvider

class TestKtScriptTemplateProvider : ScriptTemplatesProvider {
    override val dependenciesClasspath: Iterable<String>
        get() = listOf("bin/", "target/classes/")
    
    override val environment: Map<String, Any?>?
        get() = emptyMap()
    
    override val id: String
        get() = "Test"
    
    override val isValid: Boolean
        get() = true
    
    override val templateClassNames: Iterable<String>
        get() = listOf("org.jetbrains.kotlin.ui.tests.scripts.templates.TestScriptTemplateDefinition")
    
    override val version: Int
        get() = 10
}