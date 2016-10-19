package org.jetbrains.kotlin.config

open class LanguageVersionSettings {
    fun supportsFeature(feature: LanguageFeature): Boolean {
        return true
    }

    val apiVersion: ApiVersion = ApiVersion()
}

class ApiVersion {
}

class LanguageVersionSettingsImpl : LanguageVersionSettings() {
    companion object {
        val DEFAULT = LanguageVersionSettingsImpl()
    }
}