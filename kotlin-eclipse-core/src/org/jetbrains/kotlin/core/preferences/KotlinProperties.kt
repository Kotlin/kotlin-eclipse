package org.jetbrains.kotlin.core.preferences

import org.jetbrains.kotlin.core.Activator
import org.jetbrains.kotlin.config.JvmTarget
import org.eclipse.core.runtime.preferences.IScopeContext
import org.eclipse.core.runtime.preferences.DefaultScope
import org.osgi.service.prefs.Preferences as InternalPreferences
import org.eclipse.core.runtime.preferences.InstanceScope
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion

class KotlinProperties(scope: IScopeContext = InstanceScope.INSTANCE) : Preferences(scope, Activator.PLUGIN_ID) {
    var globalsOverridden by BooleanPreference()

    // Note: default value is defined in preferences.ini
    var jvmTarget by EnumPreference<JvmTarget>(JvmTarget.DEFAULT)

    var languageVersion by object : Preference<LanguageVersion> {
        override fun reader(text: String?) = text
                ?.let { LanguageVersion.fromVersionString(it) }
                ?: LanguageVersion.LATEST_STABLE

        override fun writer(value: LanguageVersion) = value.versionString
    }

    var apiVersion by object : Preference<ApiVersion> {
        override fun reader(text: String?): ApiVersion {
            val apiVersionByLanguageVersion = ApiVersion.createByLanguageVersion(languageVersion)
            return text?.let { ApiVersion.parse(it) }
                    ?.takeIf { it <= apiVersionByLanguageVersion }
                    ?: apiVersionByLanguageVersion
        }

        override fun writer(value: ApiVersion) = value.versionString
    }

    val compilerPlugins by ChildCollection(::CompilerPlugin)
    
    var compilerFlags by StringPreference()

    companion object {
        // Property object in instance scope (workspace) must be created after one in global scope (see: init())
        val workspaceInstance by lazy { KotlinProperties() }

        @JvmStatic
        fun init() {
            // Creating property object in default scope assures that values from 'preferences.ini' are loaded
            KotlinProperties(DefaultScope.INSTANCE)
        }
    }
}

class CompilerPlugin(scope: IScopeContext, path: String) : Preferences(scope, path) {
    var jarPath by StringPreference()

    var args by ListPreference()
    
    var active by BooleanPreference()
    
    // Marker property allowing overwriting plugin definitions. Should never be persisted.
    var removed: Boolean = false
}
