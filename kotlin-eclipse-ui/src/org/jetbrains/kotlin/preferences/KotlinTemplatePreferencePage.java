package org.jetbrains.kotlin.preferences;

import org.eclipse.ui.texteditor.templates.TemplatePreferencePage;
import org.jetbrains.kotlin.ui.Activator;
import org.jetbrains.kotlin.ui.editors.templates.KotlinTemplateManager;

public class KotlinTemplatePreferencePage extends TemplatePreferencePage {
    
    public KotlinTemplatePreferencePage() {
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
        setTemplateStore(KotlinTemplateManager.INSTANCE.getTemplateStore());
        
        setContextTypeRegistry(KotlinTemplateManager.INSTANCE.getContextTypeRegistry());
    }
}