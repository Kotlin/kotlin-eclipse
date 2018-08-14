package org.jetbrains.kotlin.core.script

import java.io.File
import kotlin.reflect.KClass

abstract class ScriptTemplateContribution {
    open val priority = 0

    protected abstract fun loadTemplate(): KClass<*>

    val template by lazy { loadTemplate() }

    open fun scriptEnvironment(script: File): Map<String, Any?> = emptyMap()
}