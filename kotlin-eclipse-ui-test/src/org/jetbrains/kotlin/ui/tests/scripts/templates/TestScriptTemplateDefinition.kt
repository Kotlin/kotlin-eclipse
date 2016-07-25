package org.jetbrains.kotlin.ui.tests.scripts.templates

import org.jetbrains.kotlin.script.KotlinScriptExternalDependencies
import org.jetbrains.kotlin.script.ScriptContents
import org.jetbrains.kotlin.script.ScriptDependenciesResolverEx
import org.jetbrains.kotlin.script.ScriptTemplateDefinition
import java.io.File

@ScriptTemplateDefinition(
        resolver = TestKotlinScriptResolver::class,
        scriptFilePattern = "sample.testDef.kts"
)
open class TestScriptTemplateDefinition(testNameParam: String, secondParam: Int, thirdParam: Int = 10) {
    fun doSomething() {
    }

    fun callFromBase(y: Int) {
        println(y)
    }
}

class TestKotlinScriptResolver : ScriptDependenciesResolverEx {
    override fun resolve(script: ScriptContents,
                         environment: Map<String, Any?>?,
                         previousDependencies: KotlinScriptExternalDependencies?): KotlinScriptExternalDependencies? {
        return TestScriptExternalDependencies
    }
}

object TestScriptExternalDependencies : KotlinScriptExternalDependencies {
    override val classpath: Iterable<File>
        get() = listOf()

    override val sources: Iterable<File>
        get() = listOf()

    override val imports: Iterable<String>
        get() = listOf("java.io.File", "java.util.concurrent.*")
}