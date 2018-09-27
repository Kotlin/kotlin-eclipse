package org.jetbrains.kotlin.core.formatting

import com.intellij.psi.codeStyle.CodeStyleSettings
import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.ProjectScope
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.core.preferences.KotlinCodeStyleProperties
import org.jetbrains.kotlin.idea.formatter.KotlinObsoleteCodeStyle
import org.jetbrains.kotlin.idea.formatter.KotlinPredefinedCodeStyle
import org.jetbrains.kotlin.idea.formatter.KotlinStyleGuideCodeStyle
import org.jetbrains.kotlin.psi.KtFile
import java.util.concurrent.ConcurrentHashMap

object KotlinCodeStyleManager {
    private val stylesCache = ConcurrentHashMap<String, CodeStyleSettings>()

    private val predefinedStyles: Map<String, KotlinPredefinedCodeStyle> by lazy {
        listOf(KotlinStyleGuideCodeStyle.INSTANCE, KotlinObsoleteCodeStyle.INSTANCE)
                .map { it.codeStyleId to it }
                .toMap()
    }

    val styles: List<String>
        get() = (predefinedStyles.keys + stylesCache.keys).sorted()

    // Can be used in the future to provide user defined code styles
    fun getOrCreate(id: String, settingsApplier: (CodeStyleSettings) -> Unit): CodeStyleSettings =
            stylesCache.getOrPut(id) { CodeStyleSettings().also { settingsApplier(it) } }

    fun get(id: String): CodeStyleSettings? = stylesCache[id] ?: createStyleFromPredef(id)

    // Uses the same logic as ConcurrentHashMap.getOrPut() but due to possible nullability cannot be expressed by it.
    private fun createStyleFromPredef(id: String): CodeStyleSettings? = predefinedStyles[id]
            ?.let { CodeStyleSettings().also(it::apply) }
            ?.let { stylesCache.putIfAbsent(id, it) ?: it }

    fun invalidate(id: String) {
        stylesCache -= id
    }
}

val IProject.codeStyle: CodeStyleSettings
    get() = KotlinCodeStyleProperties(ProjectScope(this))
            .codeStyleId
            ?.let { KotlinCodeStyleManager.get(it) }
            ?: CodeStyleSettings()
