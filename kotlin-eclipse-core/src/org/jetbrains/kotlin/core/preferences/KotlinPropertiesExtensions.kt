package org.jetbrains.kotlin.core.preferences

import org.jetbrains.kotlin.config.*

private enum class CompilerFlagsMapping(val flag: String, vararg val alternativeFlags: String) : (List<String>) -> Pair<AnalysisFlag<*>, *>? {
    JVM_DEFAULT("-Xjvm-default") {
        override fun invoke(value: List<String>): Pair<AnalysisFlag<JvmDefaultMode>, JvmDefaultMode>? {
            if (value.isEmpty()) return null
            val tempSingle = value.single()
            return JvmDefaultMode.fromStringOrNull(tempSingle)
                ?.let { JvmAnalysisFlags.jvmDefaultMode to it }
        }
    },
    OPT_IN("-opt-in", "-Xopt-in") {
        override fun invoke(value: List<String>): Pair<AnalysisFlag<*>, *>? {
            if (value.isEmpty()) return null
            return AnalysisFlags.optIn to value
        }
    },

    USE_XR("-Xuse-ir") {
        override fun invoke(value: List<String>): Pair<AnalysisFlag<*>, *>? {
            if (value.isEmpty()) return null
            val tempSingle = value.single()
            val tempUseIr = tempSingle.toBooleanStrictOrNull()
            return when {
                tempUseIr != null -> JvmAnalysisFlags.useIR to tempUseIr
                else -> null
            }
        }
    };

    companion object {
        fun flagByString(flag: String) = values().firstOrNull { it.flag == flag || flag in it.alternativeFlags }
    }
}

private val KotlinProperties.analyzerCompilerFlags: Map<AnalysisFlag<*>, Any?>
    get() = compilerFlags?.split("\\s+".toRegex())?.mapNotNull { flagString ->
        flagString.split("=", limit = 2).takeIf { it.size == 2 }
    }?.groupBy( { (key) ->
        key
    }, { (_, value) ->
        value
    })?.mapNotNull { (key, value) ->
        CompilerFlagsMapping.flagByString(key)?.invoke(value)
    }.orEmpty().toMap()

val KotlinProperties.languageVersionSettings: LanguageVersionSettings
    get() = LanguageVersionSettingsImpl(languageVersion, apiVersion, analyzerCompilerFlags)