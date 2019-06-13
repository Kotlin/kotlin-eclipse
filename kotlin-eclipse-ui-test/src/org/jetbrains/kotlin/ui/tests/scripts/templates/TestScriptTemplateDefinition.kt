package org.jetbrains.kotlin.ui.tests.scripts.templates

import java.io.File
import java.util.concurrent.Future
import kotlin.script.dependencies.KotlinScriptExternalDependencies
import kotlin.script.dependencies.ScriptContents
import kotlin.script.dependencies.ScriptDependenciesResolver
import kotlin.script.dependencies.asFuture
import kotlin.script.templates.ScriptTemplateDefinition

@ScriptTemplateDefinition(
        resolver = TestKotlinScriptResolver::class,
        scriptFilePattern = "sample.testDef.kts"
)
open class TestScriptTemplateDefinition(val testNameParam: String, val secondParam: Int, val thirdParam: Int = 10) {
    fun doSomething() {
    }

    fun callFromBase(y: Int) {
        println(y)
    }
}

@ScriptTemplateDefinition(
        resolver = TestKotlinScriptResolverEx::class,
        scriptFilePattern = "sampleEx.testDef.kts"
)
open class TestScriptTemplateDefinitionEx(val testNameParam: String, val secondParam: Int, val thirdParam: Int = 10)

fun TestScriptTemplateDefinition.testExtension(x: Int): String {
    return x.toString()
}

class TestKotlinScriptResolver : ScriptDependenciesResolver {
    override fun resolve(script: ScriptContents,
                         environment: Map<String, Any?>?,
                         report: (ScriptDependenciesResolver.ReportSeverity, String, ScriptContents.Position?) -> Unit,
                         previousDependencies: KotlinScriptExternalDependencies?): Future<KotlinScriptExternalDependencies?> {
        return TestScriptExternalDependencies.asFuture()
    }
}

class TestKotlinScriptResolverEx : ScriptDependenciesResolver {
    override fun resolve(script: ScriptContents,
                         environment: Map<String, Any?>?,
                         report: (ScriptDependenciesResolver.ReportSeverity, String, ScriptContents.Position?) -> Unit,
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

object TestScriptExternalDependencies : KotlinScriptExternalDependencies {
    override val classpath: Iterable<File>
        get() = listOf()

    override val sources: Iterable<File>
        get() = listOf()

    override val imports: Iterable<String>
        get() = listOf(
                "java.io.File",
                "java.util.concurrent.*",
                "org.jetbrains.kotlin.ui.tests.scripts.templates.*")
}
