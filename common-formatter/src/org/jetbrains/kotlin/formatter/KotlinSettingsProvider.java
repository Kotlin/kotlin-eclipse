package org.jetbrains.kotlin.formatter;

import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings;

import com.intellij.lang.Language;
import com.intellij.openapi.options.Configurable;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsProvider;
import com.intellij.psi.codeStyle.CustomCodeStyleSettings;

public class KotlinSettingsProvider extends CodeStyleSettingsProvider {
    @Override
    public CustomCodeStyleSettings createCustomSettings(CodeStyleSettings settings) {
        return new KotlinCodeStyleSettings(settings);
    }
    
    @Override
    public String getConfigurableDisplayName() {
        return KotlinLanguage.NAME;
    }
    
    @Override
    public Language getLanguage() {
        return KotlinLanguage.INSTANCE;
    }
    
    @Override
    public Configurable createSettingsPage(CodeStyleSettings arg0, CodeStyleSettings arg1) {
        throw new UnsupportedOperationException();
    }
}
