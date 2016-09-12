package org.jetbrains.kotlin.ui.tests.scripts.templates

import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.ResourcesPlugin
import org.jetbrains.kotlin.core.model.ScriptTemplateProviderEx
import org.jetbrains.kotlin.script.KotlinScriptExternalDependencies
import org.jetbrains.kotlin.script.ScriptContents
import org.jetbrains.kotlin.script.ScriptDependenciesResolver
import org.jetbrains.kotlin.script.ScriptTemplateDefinition
import org.jetbrains.kotlin.script.asFuture
import org.junit.Assert
import java.util.concurrent.Future

class CustomEPResolverScriptTemplateProvider : ScriptTemplateProviderEx {
    override val templateClassClasspath = listOf("bin/", "target/classes/")
    override val templateClassName = "org.jetbrains.kotlin.ui.tests.scripts.templates.CustomReolverScriptTemplateDefinition"

    override fun isApplicable(file: IFile): Boolean {
        return file.name.contains("customEPResolver")
    }
    
    override val resolver: ScriptDependenciesResolver get() = CustomScriptDependenciesResolver()
}

class CustomScriptDependenciesResolver : ScriptDependenciesResolver {
    override fun resolve(
            script: ScriptContents,
            environment: Map<String, Any?>?,
            report: (ScriptDependenciesResolver.ReportSeverity, String, ScriptContents.Position?) -> Unit,
            previousDependencies: KotlinScriptExternalDependencies?): Future<KotlinScriptExternalDependencies?> {
        return object : KotlinScriptExternalDependencies {
            override val imports: Iterable<String> get() {
                // Test workspace is available
                Assert.assertTrue(ResourcesPlugin.getWorkspace().getRoot().getLocation() != null)
                
                return listOf("java.util.Date")
            }
        }.asFuture()
    }
}

@ScriptTemplateDefinition(
        resolver = TestKotlinScriptResolver::class, // Default resolver that doesn't conform resolver in extenstion point
        scriptFilePattern = "customEPResolver.kts"
)
open class CustomReolverScriptTemplateDefinition