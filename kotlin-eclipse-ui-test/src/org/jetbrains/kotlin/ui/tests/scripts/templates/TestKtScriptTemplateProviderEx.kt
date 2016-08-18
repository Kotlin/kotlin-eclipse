package org.jetbrains.kotlin.ui.tests.scripts.templates

import org.jetbrains.kotlin.core.model.ScriptTemplateProviderEx
import org.eclipse.core.resources.IFile

class TestKtScriptTemplateProviderEx : ScriptTemplateProviderEx {
	override val dependenciesClasspath = listOf("bin/", "target/classes/")
	override val templateClassNames = listOf("org.jetbrains.kotlin.ui.tests.scripts.templates.TestScriptTemplateDefinitionEx")

	override fun isApplicable(file: IFile): Boolean {
		return file.name.contains("Ex")
	}
	
	override fun getEnvironment(file: IFile): Map<String, Any?>? {
		return super.getEnvironment(file)
	}
}