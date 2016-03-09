package com.intellij.formatting;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.KotlinLanguage;

import com.intellij.application.options.IndentOptionsEditor;
import com.intellij.lang.Language;
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider;

public class KotlinLanguageCodeStyleSettingsProvider extends LanguageCodeStyleSettingsProvider {
    @NotNull
    @Override
    public Language getLanguage() {
        return KotlinLanguage.INSTANCE;
    }

    @Override
    public String getCodeSample(@NotNull SettingsType settingsType) {
        return "";
    }

    @Override
    public String getLanguageName() {
        return KotlinLanguage.NAME;
    }

    @Override
    public void customizeSettings(@NotNull CodeStyleSettingsCustomizable consumer, @NotNull SettingsType settingsType) {
    }

    @Override
    public IndentOptionsEditor getIndentOptionsEditor() {
        return null;
    }

    @Override
    public CommonCodeStyleSettings getDefaultCommonSettings() {
        CommonCodeStyleSettings commonCodeStyleSettings = new CommonCodeStyleSettings(getLanguage());
        commonCodeStyleSettings.initIndentOptions();
        return commonCodeStyleSettings;
    }
}