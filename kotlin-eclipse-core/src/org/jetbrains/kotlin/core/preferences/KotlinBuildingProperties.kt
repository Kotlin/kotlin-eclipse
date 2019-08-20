package org.jetbrains.kotlin.core.preferences

import org.eclipse.core.runtime.preferences.IScopeContext
import org.eclipse.core.runtime.preferences.InstanceScope
import org.jetbrains.kotlin.core.Activator

class KotlinBuildingProperties(scope: IScopeContext = InstanceScope.INSTANCE) :
    Preferences(scope, Activator.PLUGIN_ID) {

    var globalsOverridden by BooleanPreference()

    var useIncremental by BooleanPreference()

    companion object {
        val workspaceInstance by lazy { KotlinBuildingProperties() }
    }
}