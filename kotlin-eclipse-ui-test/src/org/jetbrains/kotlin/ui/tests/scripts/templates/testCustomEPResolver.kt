package org.jetbrains.kotlin.ui.tests.scripts.templates

import org.eclipse.core.resources.IFile
import org.eclipse.core.runtime.IProgressMonitor
import org.jetbrains.kotlin.core.model.ScriptTemplateProviderEx
import org.jetbrains.kotlin.script.KotlinScriptExternalDependencies
import org.jetbrains.kotlin.script.ScriptContents
import org.jetbrains.kotlin.script.ScriptDependenciesResolver
import org.jetbrains.kotlin.script.ScriptTemplateDefinition
import org.jetbrains.kotlin.script.asFuture
import java.util.concurrent.Future

class CustomEPResolverScriptTemplateProvider : ScriptTemplateProviderEx {
    override val templateClassName = "org.jetbrains.kotlin.ui.tests.scripts.templates.CustomReolverScriptTemplateDefinition"
    
    override fun getTemplateClasspath(environment: Map<String, Any?>?, monitor: IProgressMonitor): Iterable<String> {
        return listOf("bin/", "target/classes/")
    }

    override fun getEnvironment(file: IFile): Map<String, Any?>? = null
}

@ScriptTemplateDefinition(
        resolver = CustomScriptDependenciesResolver::class,
        scriptFilePattern = "customEPResolver.kts"
)
open class CustomReolverScriptTemplateDefinition

class CustomScriptDependenciesResolver : ScriptDependenciesResolver {
    override fun resolve(
            script: ScriptContents,
            environment: Map<String, Any?>?,
            report: (ScriptDependenciesResolver.ReportSeverity, String, ScriptContents.Position?) -> Unit,
            previousDependencies: KotlinScriptExternalDependencies?): Future<KotlinScriptExternalDependencies?> {
        return object : KotlinScriptExternalDependencies {
            override val imports: Iterable<String> get() = listOf("java.util.Date")
        }.asFuture()
    }
}