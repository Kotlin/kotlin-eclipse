package org.jetbrains.kotlin.ui.editors.templates;

import org.eclipse.jface.text.templates.GlobalTemplateVariables;
import org.eclipse.jface.text.templates.TemplateContextType;

public class KotlinTemplateContextType extends TemplateContextType {
    
    public static final String CONTEXT_TYPE_REGISTRY = "org.jetbrains.kotlin.ui.editors.KotlinEditor";
    public static final String KOTLIN_ID_TOP_LEVEL_DECLARATIONS = "kotlin-top-level-declarations";

    public KotlinTemplateContextType() {
        addResolver(new GlobalTemplateVariables.Cursor());
        addResolver(new GlobalTemplateVariables.WordSelection());
        addResolver(new GlobalTemplateVariables.Dollar());
        addResolver(new GlobalTemplateVariables.Date());
        addResolver(new GlobalTemplateVariables.Year());
        addResolver(new GlobalTemplateVariables.Time());
        addResolver(new GlobalTemplateVariables.User());
    }
}
