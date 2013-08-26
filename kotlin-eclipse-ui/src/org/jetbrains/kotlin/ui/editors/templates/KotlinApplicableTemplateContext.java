package org.jetbrains.kotlin.ui.editors.templates;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.templates.Template;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.utils.LineEndUtil;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

public class KotlinApplicableTemplateContext {
    
    @NotNull
    public static List<Template> getTemplatesByContextTypeIds(List<String> contextTypeIds) {
        List<Template> possibleTeplates = new ArrayList<Template>(); 
        for (String contextTypeId : contextTypeIds) {
            Template[] templates = KotlinTemplateManager.INSTANCE.getTemplateStore().getTemplates(contextTypeId);
            for (Template template : templates) {
                possibleTeplates.add(template);
            }
        }
        
        return possibleTeplates;
    }
    
    @NotNull
    public static List<String> getApplicableContextTypeIds(@NotNull ITextViewer viewer, @NotNull IFile file, int offset) {
        int offsetWithoutCr = LineEndUtil.convertCrToOsOffset(viewer.getDocument().get(), offset);
        PsiFile psiFile = KotlinPsiManager.INSTANCE.getParsedFile(file);
        
        List<String> contextTypeIds = new ArrayList<String>();
        for (String contextTypeId : getAllContextTypeIds()) {
            PsiElement psiElement = psiFile.findElementAt(offsetWithoutCr - 1);
            if (isContextApplicableAt(psiElement, contextTypeId)) {
                contextTypeIds.add(contextTypeId);
            }
        }
        
        return contextTypeIds;
    }
    
    private static boolean isContextApplicableAt(@Nullable PsiElement psiElement, @NotNull String contextType) {
        if (psiElement == null) {
            if (KotlinTemplateContextType.KOTLIN_ID_TOP_LEVEL_DECLARATIONS.equals(contextType)) {
                return true;
            }
        } else {
            psiElement = psiElement.getParent();
            if (psiElement instanceof JetFile) {
                if (KotlinTemplateContextType.KOTLIN_ID_TOP_LEVEL_DECLARATIONS.equals(contextType)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    @NotNull
    private static Set<String> getAllContextTypeIds() {
        Set<String> contextTypeIds = new HashSet<String>();
        
        Template[] templates = KotlinTemplateManager.INSTANCE.getTemplateStore().getTemplates();
        for (Template template : templates) {
            String contextTypeId = template.getContextTypeId();
            if (!contextTypeIds.contains(contextTypeId)) {
                contextTypeIds.add(contextTypeId);
            }
        }
        
        return contextTypeIds;
    }
}
