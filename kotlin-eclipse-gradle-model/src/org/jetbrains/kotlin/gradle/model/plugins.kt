package org.jetbrains.kotlin.gradle.model

import java.io.Serializable

interface CompilerPluginConfig: Serializable {
    val pluginName: String
    val options: List<String>
}

sealed class CompilerPluginConfigImpl(
    shortName: String,
    fqName: String,
    knownPresets: Collection<String>,
    declaredAnnotations: Collection<String>,
    declaredPresets: Collection<String>,
    otherOptions: List<String> = emptyList()
) : CompilerPluginConfig {
    final override val pluginName: String = knownPresets.find { it in declaredPresets } ?: shortName

    override val options: List<String> =
        (declaredPresets - pluginName).map { "$fqName:preset=$it" } +
                declaredAnnotations.map { "$fqName:annotation=$it" } +
                otherOptions
}

class AllOpen(
    annotations: Collection<String>,
    presets: Collection<String>
) : CompilerPluginConfigImpl(
    shortName = "all-open",
    fqName = "org.jetbrains.kotlin.allopen",
    knownPresets = listOf("spring"),
    declaredAnnotations = annotations,
    declaredPresets = presets
)

class NoArg(
    annotations: Collection<String>,
    presets: Collection<String>,
    invokeInitializers: Boolean?
) : CompilerPluginConfigImpl(
    shortName = "no-arg",
    fqName = "org.jetbrains.kotlin.noarg",
    knownPresets = listOf("jpa"),
    declaredAnnotations = annotations,
    declaredPresets = presets,
    otherOptions = invokeInitializers?.let { listOf("org.jetbrains.kotlin.noarg:invokeInitializers=$it") }.orEmpty()
)

class SAMWithReceiver(
    annotations: Collection<String>,
    presets: Collection<String>
) : CompilerPluginConfigImpl(
    shortName = "sam-with-receiver",
    fqName = "org.jetbrains.kotlin.samWithReceiver",
    knownPresets = emptyList(),
    declaredAnnotations = annotations,
    declaredPresets = presets
)
