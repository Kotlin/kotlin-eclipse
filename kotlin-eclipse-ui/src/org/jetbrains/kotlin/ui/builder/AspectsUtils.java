package org.jetbrains.kotlin.ui.builder;

import org.eclipse.core.resources.IFile;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;

public class AspectsUtils {
    public static boolean isKotlinFile(IFile file) {
        return KotlinPsiManager.isKotlinFile(file);
    }
    
    public static boolean existsSourceFile(IFile file) {
        return KotlinPsiManager.INSTANCE.existsSourceFile(file);
    }
}
