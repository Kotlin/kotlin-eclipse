package org.jetbrains.kotlin.ui.tests.scripts.templates

import org.jetbrains.kotlin.core.model.ScriptTemplateProviderEx
import org.eclipse.core.resources.IFile

class TestKtScriptTemplateProviderEx : ScriptTemplateProviderEx {
    override val templateClassName = "org.jetbrains.kotlin.ui.tests.scripts.templates.TestScriptTemplateDefinitionEx"
    
    override fun getTemplateClasspath(environment: Map<String, Any?>?): Iterable<String> {
        return listOf("bin/", "target/classes/")
    }

    override fun getEnvironment(file: IFile): Map<String, Any?>? {
        return mapOf(
                "projectName" to file.project.name,
                "additionalImports" to arrayOf("java.util.Date") 
        )
    }
}