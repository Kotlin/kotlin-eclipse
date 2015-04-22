package org.jetbrains.kotlin.ui.editors.quickassist;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
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
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil;
import org.jetbrains.kotlin.ui.editors.AnnotationManager;
import org.jetbrains.kotlin.ui.editors.DiagnosticAnnotation;
import org.jetbrains.kotlin.ui.editors.DiagnosticAnnotationUtil;
import org.jetbrains.kotlin.ui.editors.KotlinEditor;
import org.jetbrains.kotlin.ui.editors.quickfix.KotlinSearchTypeRequestor;

import com.google.common.collect.Lists;
import com.intellij.psi.PsiElement;

public class KotlinAutoImportProposalsGenerator extends KotlinQuickAssistProposalsGenerator {
    @Override
    @NotNull
    protected List<KotlinQuickAssistProposal> getProposals(@NotNull KotlinEditor kotlinEditor,
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
        KotlinEditor editor = getActiveEditor();
        if (editor == null) {
            return false;
        }
        
        int caretOffset = getCaretOffset(editor);
        DiagnosticAnnotation annotation = DiagnosticAnnotationUtil.INSTANCE.getAnnotationByOffset(editor, caretOffset);
        if (annotation != null) {
            DiagnosticFactory<?> diagnostic = annotation.getDiagnostic();
            return diagnostic != null ? DiagnosticAnnotationUtil.isUnresolvedReference(diagnostic) : false;
        }
        
        IFile file = EditorUtil.getFile(editor);
        if (file == null) {
            KotlinLogger.logError("Failed to retrieve IFile from editor " + editor, null);
            return false;
        }
        
        IMarker marker = DiagnosticAnnotationUtil.INSTANCE.getMarkerByOffset(file, caretOffset);
        return marker != null ? marker.getAttribute(AnnotationManager.IS_UNRESOLVED_REFERENCE, false) : false;
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
