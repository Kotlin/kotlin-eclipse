package com.intellij.formatting

import org.jetbrains.kotlin.idea.KotlinLanguage
import com.intellij.application.options.IndentOptionsEditor
import com.intellij.lang.Language
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider
import org.jetbrains.kotlin.idea.formatter.KotlinCommonCodeStyleSettings

class KotlinLanguageCodeStyleSettingsProvider : LanguageCodeStyleSettingsProvider() {
    override fun getLanguage(): Language = KotlinLanguage.INSTANCE
    
    override fun getCodeSample(settingsType: SettingsType): String = ""
    
    override fun getLanguageName(): String = KotlinLanguage.NAME
    
    override fun customizeSettings(consumer:CodeStyleSettingsCustomizable, settingsType:SettingsType) {
    }
    
    override fun getIndentOptionsEditor(): IndentOptionsEditor? = null
    
    override fun getDefaultCommonSettings(): CommonCodeStyleSettings {
        val commonCodeStyleSettings = KotlinCommonCodeStyleSettings()
        commonCodeStyleSettings.initIndentOptions()
        return commonCodeStyleSettings
    }
}