package org.jetbrains.kotlin.core.preferences

import org.jetbrains.kotlin.core.Activator
import org.jetbrains.kotlin.config.JvmTarget
import org.eclipse.core.runtime.preferences.IScopeContext
import org.eclipse.core.runtime.preferences.DefaultScope
import org.osgi.service.prefs.Preferences as InternalPreferences

class KotlinProperties(scope: IScopeContext = DefaultScope.INSTANCE) : Preferences(Activator.PLUGIN_ID, scope) {
    var jvmTarget by EnumPreference<JvmTarget>()

    val compilerPlugins by ChildCollection(::CompilerPlugin)
}

class CompilerPlugin(internal: InternalPreferences) : Preferences(internal) {
    var jarPath by StringPreference()

    var args by ListPreference()
    
    var active by BooleanPreference()
    
    // Marker property allowing overwriting plugin definitions. Should never be persisted.
    var removed: Boolean = false
}