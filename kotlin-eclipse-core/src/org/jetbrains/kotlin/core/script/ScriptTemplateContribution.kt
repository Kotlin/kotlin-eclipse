package org.jetbrains.kotlin.core.script

import java.io.File
import kotlin.reflect.KClass
import kotlin.script.experimental.api.ScriptCompilationConfiguration

abstract class ScriptTemplateContribution {
    open val priority = 0

    protected abstract fun loadTemplate(): KClass<*>

    val template by lazy { loadTemplate() }

    open fun isNullable(propName: String, compilationConfig: ScriptCompilationConfiguration): Boolean = true

    open fun scriptEnvironment(script: File): Map<String, Any?> = emptyMap()
}

abstract class JavaScriptTemplateContribution : ScriptTemplateContribution() {

    abstract val javaClass: Class<*>

    override fun loadTemplate(): KClass<*> = javaClass.kotlin
}