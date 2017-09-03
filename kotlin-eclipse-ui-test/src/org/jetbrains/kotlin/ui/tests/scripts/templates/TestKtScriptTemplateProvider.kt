package org.jetbrains.kotlin.ui.tests.scripts.templates

import org.jetbrains.kotlin.script.ScriptTemplatesProvider
import java.io.File

class TestKtScriptTemplateProvider : ScriptTemplatesProvider {
    override val templateClasspath: List<File>
        get() = listOf()

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