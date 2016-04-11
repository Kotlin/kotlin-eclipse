package org.jetbrains.kotlin.formatter;

import org.jetbrains.kotlin.idea.KotlinLanguage;

import com.intellij.application.options.IndentOptionsEditor;
import com.intellij.lang.Language;
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider;

public class KotlinLanguageCodeStyleSettingsProvider extends LanguageCodeStyleSettingsProvider {

    @Override
    public String getCodeSample(SettingsType arg0) {
        return "";
    }

    @Override
    public Language getLanguage() {
        return KotlinLanguage.INSTANCE;
    }

    @Override
    public void customizeSettings(CodeStyleSettingsCustomizable consumer, SettingsType settingsType) {
    }

    @Override
    public CommonCodeStyleSettings getDefaultCommonSettings() {
        CommonCodeStyleSettings commonCodeStyleSettings = new CommonCodeStyleSettings(getLanguage());
        commonCodeStyleSettings.initIndentOptions();
        return commonCodeStyleSettings;
    }

    @Override
    public IndentOptionsEditor getIndentOptionsEditor() {
        return null;
    }

    @Override
    public String getLanguageName() {
        return KotlinLanguage.NAME;
    }
}
