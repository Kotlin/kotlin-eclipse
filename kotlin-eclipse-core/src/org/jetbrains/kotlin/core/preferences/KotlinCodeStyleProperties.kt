package org.jetbrains.kotlin.core.preferences

import org.eclipse.core.resources.ProjectScope
import org.eclipse.core.runtime.preferences.IScopeContext
import org.eclipse.core.runtime.preferences.InstanceScope
import org.jetbrains.kotlin.core.Activator
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.core.formatting.KotlinCodeStyleManager
import org.jetbrains.kotlin.idea.formatter.KotlinPredefinedCodeStyle
import org.jetbrains.kotlin.idea.formatter.KotlinStyleGuideCodeStyle
import org.jetbrains.kotlin.psi.KtFile

class KotlinCodeStyleProperties(scope: IScopeContext = InstanceScope.INSTANCE)
    : Preferences(scope, "${Activator.PLUGIN_ID}/codeStyle"
) {
    var globalsOverridden by BooleanPreference()

    var codeStyleId by StringPreference()

    companion object {
        val workspaceInstance by lazy { KotlinCodeStyleProperties() }
    }
}