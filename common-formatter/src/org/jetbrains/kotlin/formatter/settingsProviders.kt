//package org.jetbrains.kotlin.formatter
//
//import com.intellij.lang.Language
//import com.intellij.openapi.options.Configurable
//import com.intellij.psi.codeStyle.CodeStyleSettings
//import com.intellij.psi.codeStyle.CustomCodeStyleSettings
//import org.jetbrains.kotlin.idea.KotlinLanguage
//import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings
//import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider
//import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable
//import com.intellij.application.options.IndentOptionsEditor
//import com.intellij.psi.codeStyle.CommonCodeStyleSettings
//import com.intellij.psi.codeStyle.CodeStyleSettingsProvider
//
//class KotlinSettingsProvider : CodeStyleSettingsProvider() {
//    override fun getConfigurableDisplayName(): String = KotlinLanguage.NAME
//    
//    override fun getLanguage(): Language = KotlinLanguage.INSTANCE
//    
//    override fun createCustomSettings(settings: CodeStyleSettings): CustomCodeStyleSettings {
//        return KotlinCodeStyleSettings(settings)
//    }
//    
//    override fun createSettingsPage(settings: CodeStyleSettings, originalSettings: CodeStyleSettings): Configurable {
//        throw UnsupportedOperationException()
//    }
//}
//
//class KotlinLanguageCodeStyleSettingsProvider : LanguageCodeStyleSettingsProvider() {
//    override fun getLanguage(): Language = KotlinLanguage.INSTANCE
//    
//    override fun getCodeSample(settingsType: SettingsType): String = ""
//    
//    override fun getLanguageName(): String = KotlinLanguage.NAME
//    
//    override fun customizeSettings(consumer: CodeStyleSettingsCustomizable, settingsType: SettingsType) {
//    }
//    
//    override fun getIndentOptionsEditor(): IndentOptionsEditor? = null
//    
//    override fun getDefaultCommonSettings(): CommonCodeStyleSettings {
//        val commonCodeStyleSettings = CommonCodeStyleSettings(language)
//        commonCodeStyleSettings.initIndentOptions()
//        return commonCodeStyleSettings
//    }
//}