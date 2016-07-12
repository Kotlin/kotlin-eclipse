package org.jetbrains.kotlin.ui.builder;

import org.eclipse.core.resources.IFile;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.model.KotlinScriptEnvironment;

public class AspectsUtils {
    public static boolean isKotlinFile(IFile file) {
        return KotlinPsiManager.isKotlinFile(file);
    }
    
    public static boolean existsSourceFile(IFile file) {
        return KotlinPsiManager.INSTANCE.existsSourceFile(file);
    }
    
    public static boolean isScript(IFile file) {
        return KotlinScriptEnvironment.Companion.isScript(file);
    }
}
