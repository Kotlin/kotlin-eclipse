package org.jetbrains.test.scriptProvider

import org.eclipse.core.resources.IFile
import org.eclipse.core.runtime.IProgressMonitor
import org.jetbrains.kotlin.core.model.ScriptTemplateProviderEx
import org.jetbrains.kotlin.script.KotlinScriptExternalDependencies
import org.jetbrains.kotlin.script.ScriptContents
import org.jetbrains.kotlin.script.ScriptDependenciesResolver
import org.jetbrains.kotlin.script.ScriptDependenciesResolver.ReportSeverity
import org.jetbrains.kotlin.script.ScriptTemplateDefinition
import java.util.concurrent.Future
import org.jetbrains.kotlin.script.asFuture

class MyGradleProvider : ScriptTemplateProviderEx {
    override val templateClassName: String
        get() = "org.jetbrains.test.scriptProvider.TestScriptTemplateDefinitionEx"

    override fun getTemplateClasspath(environment: Map<String, Any?>?, monitor: IProgressMonitor): Iterable<String> {
        return listOf("/Users/jetbrains/projects/tests/forScripts/kotlin_core.jar"
//                "/Users/jetbrains/projects/kotlin-eclipse/kotlin-eclipse-core/bin/",
//                "/Users/jetbrains/projects/kotlin-eclipse/kotlin-eclipse-ui/bin/",
//                "/Users/jetbrains/projects/kotlin-eclipse/kotlin-bundled-compiler/bin/"
        )
    }

    override fun getEnvironment(file: IFile): Map<String, Any?>? {
        return mapOf(
                "projectName" to file.project.name,
                "additionalImports" to arrayOf("java.util.Date") 
        )
    }
}

@ScriptTemplateDefinition(
        resolver = TestKotlinScriptResolverEx::class,
        scriptFilePattern = "sampleEx.testDef.kts"
)
open class TestScriptTemplateDefinitionEx(val testNameParam: String, val secondParam: Int, val thirdParam: Int = 10) {
    fun callFromBase() {}
}

class TestKotlinScriptResolverEx : ScriptDependenciesResolver {
    override fun resolve(script: ScriptContents,
                         environment: Map<String, Any?>?,
                         report: (ReportSeverity, String, ScriptContents.Position?) -> Unit,
                         previousDependencies: KotlinScriptExternalDependencies?): Future<KotlinScriptExternalDependencies?> {
        val additionalImports = if (environment != null) {
            @Suppress("UNCHECKED_CAST")
            val importsArray = environment["additionalImports"] as? Array<String> 
            importsArray?.toList()
        } else {
            null
        }

        val standardImports = listOf(
                "java.io.File",
                "java.util.concurrent.*",
                "org.jetbrains.kotlin.ui.tests.scripts.templates.*") 
        
        return object : KotlinScriptExternalDependencies {
            override val imports: Iterable<String>
                get() =  standardImports + (additionalImports ?: emptyList())
        }.asFuture()
    }
}