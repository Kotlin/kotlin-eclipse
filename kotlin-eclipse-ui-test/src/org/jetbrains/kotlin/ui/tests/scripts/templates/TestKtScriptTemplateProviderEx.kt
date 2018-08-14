package org.jetbrains.kotlin.ui.tests.scripts.templates

import org.eclipse.core.resources.IFile
import org.eclipse.core.runtime.IProgressMonitor

//import org.jetbrains.kotlin.core.model.ScriptTemplateProviderEx
//
//class TestKtScriptTemplateProviderEx : ScriptTemplateProviderEx {
//    override val templateClassName = "org.jetbrains.kotlin.ui.tests.scripts.templates.TestScriptTemplateDefinitionEx"
//    
//    override fun getTemplateClasspath(environment: Map<String, Any?>?, monitor: IProgressMonitor): Iterable<String> {
//        return listOf("bin/", "target/classes/")
//    }
//
//    override fun getEnvironment(file: IFile): Map<String, Any?>? {
//        return mapOf(
//                "projectName" to file.project.name,
//                "additionalImports" to arrayOf("java.util.Date") 
//        )
//    }
//}