package org.jetbrains.kotlin.core.preferences

import org.jetbrains.kotlin.core.Activator
import org.jetbrains.kotlin.config.JvmTarget
import org.eclipse.core.runtime.preferences.IScopeContext
import org.eclipse.core.runtime.preferences.DefaultScope
import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.ProjectScope

class KotlinProperties(scope: IScopeContext = DefaultScope.INSTANCE) : Preferences(Activator.PLUGIN_ID, scope) {
	var jvmTarget by EnumPreference<JvmTarget>()
}