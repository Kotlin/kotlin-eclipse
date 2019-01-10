package org.jetbrains.kotlin.core.preferences

import org.jetbrains.kotlin.config.AnalysisFlag
import org.jetbrains.kotlin.config.JvmDefaultMode
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.config.JvmAnalysisFlags
import org.jetbrains.kotlin.utils.ReportLevel
import kotlin.reflect.jvm.internal.impl.utils.Jsr305State

private enum class CompilerFlagsMapping(val flag: String) : (String) -> Pair<AnalysisFlag<*>, *>? {
    JVM_DEFAULT("-Xjvm-default") {
        override fun invoke(value: String) =
            JvmDefaultMode.fromStringOrNull(value)
                ?.let { JvmAnalysisFlags.jvmDefaultMode to it }
    },
    JSR_305("-Xjsr305") {
        override fun invoke(value: String) =
            when (ReportLevel.findByDescription(value)) {
                ReportLevel.IGNORE -> Jsr305State.DISABLED
                ReportLevel.WARN -> Jsr305State.DEFAULT
                ReportLevel.STRICT -> Jsr305State.STRICT
                else -> null
            }?.let { JvmAnalysisFlags.jsr305 to it }
    };

    companion object {
        fun flagByString(flag: String) = values().firstOrNull { it.flag == flag }
    }
}

private val KotlinProperties.analyzerCompilerFlags: Map<AnalysisFlag<*>, Any?>
    get() = compilerFlags?.split("\\s+".toRegex())?.mapNotNull { flagString ->
        flagString.split("=", limit = 2).takeIf { it.size == 2 }
    }?.mapNotNull { (key, value) ->
        CompilerFlagsMapping.flagByString(key)?.invoke(value)
    }.orEmpty().toMap()

val KotlinProperties.languageVersionSettings: LanguageVersionSettings
    get() = LanguageVersionSettingsImpl(languageVersion, apiVersion, analyzerCompilerFlags)