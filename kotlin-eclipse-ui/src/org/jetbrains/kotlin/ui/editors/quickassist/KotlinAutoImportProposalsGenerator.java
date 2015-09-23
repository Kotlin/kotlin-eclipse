package org.jetbrains.kotlin.ui.editors.quickassist;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
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
            
            String text = psiElement.getText();
            List<IMember> members = KotlinIntentionUtilsKt.unionMembers(findAllTypes(text), findAllMethods(text));
            for (IMember member : members) {
                if (Flags.isPublic(member.getFlags())) {
                    assistProposals.add(new KotlinAutoImportAssistProposal(member));
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
    
    private List<IMethod> findAllMethods(@NotNull String methodName) {
        final List<IMethod> methods = new ArrayList<>();
        
        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        IPackageFragmentRoot[] roots = KotlinIntentionUtilsKt.obtainKotlinPackageFragmentRoots(projects);
        IJavaSearchScope scope = SearchEngine.createJavaSearchScope(roots);
        
        SearchPattern pattern = SearchPattern.createPattern(methodName, IJavaSearchConstants.METHOD, 
                IJavaSearchConstants.DECLARATIONS, SearchPattern.R_EXACT_MATCH);
        
        SearchEngine searchEngine = new SearchEngine();
        try {
            searchEngine.search(
                    pattern, 
                    new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() }, 
                    scope, 
                    new SearchRequestor() {
                        @Override
                        public void acceptSearchMatch(SearchMatch match) throws CoreException {
                            Object element = match.getElement();
                            if (element instanceof IMethod) {
                                methods.add((IMethod) element);
                            }
                        }
                    }, 
                    null);
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
        
        return methods;
    }
}
