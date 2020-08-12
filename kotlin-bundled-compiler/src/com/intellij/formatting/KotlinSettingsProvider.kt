package com.intellij.formatting

import com.intellij.lang.Language
import com.intellij.openapi.options.Configurable
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CodeStyleSettingsProvider
import com.intellij.psi.codeStyle.CustomCodeStyleSettings
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings

class KotlinSettingsProvider : CodeStyleSettingsProvider() {
    override fun getConfigurableDisplayName(): String = KotlinLanguage.NAME
    
    override fun getLanguage(): Language = KotlinLanguage.INSTANCE
    
    override fun createCustomSettings(settings: CodeStyleSettings): CustomCodeStyleSettings {
        return KotlinCodeStyleSettings(settings).apply {
            this.ALLOW_TRAILING_COMMA = true
            this.ALLOW_TRAILING_COMMA_ON_CALL_SITE = true
        }
    }
    
    override fun createSettingsPage(settings: CodeStyleSettings, originalSettings: CodeStyleSettings): Configurable {
        throw UnsupportedOperationException()
    }
}