package org.jetbrains.kotlin.ui.editors.quickassist;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor;

import com.intellij.psi.PsiElement;

public abstract class KotlinQuickAssistProposalsGenerator extends KotlinQuickAssist {
    @NotNull
    public List<KotlinQuickAssistProposal> getProposals() {
        KotlinFileEditor activeEditor = getActiveEditor();
        if (activeEditor == null) {
            return Collections.emptyList();
        }
        
        PsiElement activeElement = getActiveElement();
        if (activeElement == null) {
            return Collections.emptyList();
        }
        
        return getProposals(activeEditor, activeElement);
    }
    
    public boolean hasProposals() {
        return !getProposals().isEmpty();
    }
    
    @NotNull
    protected abstract List<KotlinQuickAssistProposal> getProposals(@NotNull KotlinFileEditor kotlinFileEditor, @NotNull PsiElement psiElement);
}
