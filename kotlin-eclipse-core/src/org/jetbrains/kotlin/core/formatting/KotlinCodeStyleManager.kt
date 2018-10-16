package org.jetbrains.kotlin.core.formatting

import com.intellij.psi.codeStyle.CodeStyleSettings
import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.ProjectScope
import org.jetbrains.kotlin.core.model.EPAttribute
import org.jetbrains.kotlin.core.model.ExecutableExtensionPointDescriptor
import org.jetbrains.kotlin.core.model.loadExecutableEP
import org.jetbrains.kotlin.core.preferences.KotlinCodeStyleProperties
import org.jetbrains.kotlin.core.utils.pairOfNotNulls
import org.jetbrains.kotlin.idea.formatter.KotlinPredefinedCodeStyle
import java.util.concurrent.ConcurrentHashMap

private const val CODESTYLE_EXTENSION_POINT = "org.jetbrains.kotlin.core.predefinedKotlinCodeStyle"

object KotlinCodeStyleManager {
    private val extensions by lazy {
        loadExecutableEP<KotlinPredefinedCodeStyle>(CODESTYLE_EXTENSION_POINT, isCaching = true)
    }

    private val stylesCache = ConcurrentHashMap<String, CodeStyleSettings>()

    private val predefinedStyles: Map<String, KotlinPredefinedCodeStyle> by lazy {
        extensions
            .mapNotNull { it.createProvider() }
            .map { it.codeStyleId to it }
            .toMap()
    }

    val buildsystemAliases by lazy {
        extensions.mapNotNull { pairOfNotNulls(it.nameInBuildsystem, it.createProvider()?.name) }
            .toMap()
    }

    val styles: List<String>
        get() = (predefinedStyles.keys + stylesCache.keys).sorted()

    // Can be used in the future to provide user defined code styles
    fun getOrCreate(id: String, settingsApplier: CodeStyleSettings.() -> Unit): CodeStyleSettings =
        stylesCache.getOrPut(id) { CodeStyleSettings().also { it.settingsApplier() } }

    fun get(id: String): CodeStyleSettings? = stylesCache[id] ?: createStyleFromPredef(id)
    
    // Uses the same logic as ConcurrentHashMap.getOrPut() but due to possible nullability cannot be expressed by that method.
    private fun createStyleFromPredef(id: String): CodeStyleSettings? = predefinedStyles[id]
        ?.let { CodeStyleSettings().also(it::apply) }
        ?.let { stylesCache.putIfAbsent(id, it) ?: it }

    fun invalidate(id: String) {
        stylesCache -= id
    }

    fun getStyleLabel(id: String?) =
        id?.let { predefinedStyles[it]?.name ?: it } ?: "unknown"
}

private val IProject.codeStyleSettings
    get() = KotlinCodeStyleProperties(ProjectScope(this))
        .takeIf { it.globalsOverridden }
        ?: KotlinCodeStyleProperties.workspaceInstance

val IProject.codeStyle: CodeStyleSettings
    get() = codeStyleSettings
        .codeStyleId
        ?.let { KotlinCodeStyleManager.get(it) }
        ?: CodeStyleSettings()

private val ExecutableExtensionPointDescriptor<KotlinPredefinedCodeStyle>.nameInBuildsystem: String? by EPAttribute