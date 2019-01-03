package org.jetbrains.kotlin.core.preferences

import org.eclipse.core.runtime.preferences.DefaultScope
import org.eclipse.core.runtime.preferences.IScopeContext
import org.eclipse.core.runtime.preferences.InstanceScope
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.core.Activator
import org.osgi.service.prefs.Preferences as InternalPreferences
import kotlin.reflect.jvm.internal.impl.utils.Jsr305State
import org.jetbrains.kotlin.utils.ReportLevel

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

    val analyzerCompilerFlags: Map<AnalysisFlag<*>, Any?>
        get() = compilerFlags?.split("\\s+".toRegex())?.mapNotNull { flagString ->
            flagString.split("=", limit = 2).takeIf { pair ->
                pair.size == 2
            }?.let { pair ->
                CompilerFlagsMapping.analysisFlagsMapping[pair[0]]?.invoke(pair[1])
            }
        }?.toMap<AnalysisFlag<*>, Any?>() ?: emptyMap()

    internal object CompilerFlagsMapping {
        private fun createJvmDefaultModeFlag(value: String) =
                AnalysisFlag.jvmDefaultMode to
                        JvmDefaultMode.fromStringOrNull(value)

        private fun createJsr305Flag(value: String) =
                AnalysisFlag.jsr305 to
                        when (ReportLevel.findByDescription(value)) {
                            ReportLevel.IGNORE -> Jsr305State.DISABLED
                            ReportLevel.STRICT -> Jsr305State.STRICT
                            else -> Jsr305State.DEFAULT
                        }

        val analysisFlagsMapping = mapOf(
                ("-Xjvm-default" to ::createJvmDefaultModeFlag),
                ("-Xjsr305" to ::createJsr305Flag)
        )
    }

    companion object {
        // Property object in instance scope (workspace) must be created after init()
        val workspaceInstance by lazy { KotlinProperties() }

        @JvmStatic
        fun init() {
            // Ensure 'preferences.ini' are loaded
            DefaultScope.INSTANCE.getNode(Activator.PLUGIN_ID)
        }
    }
}

class CompilerPlugin(scope: IScopeContext, path: String) : Preferences(scope, path) {
    var jarPath by StringPreference()

    var args by ListPreference()

    var active by BooleanPreference()
}

val KotlinProperties.languageVersionSettings: LanguageVersionSettings
    get() = LanguageVersionSettingsImpl(languageVersion, apiVersion, analyzerCompilerFlags)