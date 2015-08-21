package org.jetbrains.kotlin.ui.editors.quickassist;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.TypeNameMatchRequestor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.ui.editors.AnnotationManager;
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor;
import org.jetbrains.kotlin.ui.editors.quickfix.KotlinSearchTypeRequestor;

import com.google.common.collect.Lists;
import com.intellij.psi.PsiElement;

public class KotlinAutoImportProposalsGenerator extends KotlinQuickAssistProposalsGenerator {
    private final DiagnosticFactory<?> errorForQuickFix = Errors.UNRESOLVED_REFERENCE;
    private final String markerAnnotationForFix = AnnotationManager.IS_UNRESOLVED_REFERENCE;
    
    @Override
    @NotNull
    protected List<KotlinQuickAssistProposal> getProposals(@NotNull KotlinFileEditor kotlinFileEditor,
            @NotNull PsiElement psiElement) {
        List<KotlinQuickAssistProposal> assistProposals = Lists.newArrayList();
        try {
            for (IType type : findAllTypes(psiElement.getText())) {
                if (Flags.isPublic(type.getFlags())) {
                    assistProposals.add(new KotlinAutoImportAssistProposal(type));
                }
            }
        } catch (JavaModelException e) {
            KotlinLogger.logAndThrow(e);
        }
        
        return assistProposals;
    }

    @Override
    public boolean isApplicable(@NotNull PsiElement psiElement) {
        return isDiagnosticActiveForElement(psiElement, errorForQuickFix, markerAnnotationForFix);
    }
    
    @NotNull
    private List<IType> findAllTypes(@NotNull String typeName) {
        IJavaSearchScope scope = SearchEngine.createWorkspaceScope();
        List<IType> searchCollector = new ArrayList<IType>();
        TypeNameMatchRequestor requestor = new KotlinSearchTypeRequestor(searchCollector);
        try {
            SearchEngine searchEngine = new SearchEngine(); 
            searchEngine.searchAllTypeNames(null, 
                    SearchPattern.R_EXACT_MATCH, 
                    typeName.toCharArray(), 
                    SearchPattern.R_EXACT_MATCH, 
                    IJavaSearchConstants.TYPE, 
                    scope, 
                    requestor,
                    IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, 
                    null);
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
        
        return searchCollector;
    }
}
