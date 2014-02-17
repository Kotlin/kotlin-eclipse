package org.jetbrains.kotlin.ui.editors.completion;

import java.util.List;

import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.utils.EditorUtil;
import org.jetbrains.kotlin.utils.LineEndUtil;

import com.google.common.collect.Lists;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;

public class KotlinCompletionUtils {

    public static final KotlinCompletionUtils INSTANCE = new KotlinCompletionUtils();
    
    private KotlinCompletionUtils() {
    }
    
    @NotNull
    public List<ICompletionProposal> filterCompletionProposals(@NotNull List<ICompletionProposal> proposals, @NotNull String prefix, int identOffset) {
        List<ICompletionProposal> filteredProposals = Lists.newArrayList();
        for (ICompletionProposal proposal : proposals) {
            String displayString = proposal.getDisplayString();
            String proposalString = proposal.getAdditionalProposalInfo();
            
            String replacementString = proposalString == null ? displayString : proposalString;
            
            if (replacementString.startsWith(prefix) || replacementString.toLowerCase().startsWith(prefix) || 
                    SearchPattern.camelCaseMatch(prefix, replacementString)) {
                filteredProposals.add(new CompletionProposal(replacementString, identOffset, prefix.length(), 
                        replacementString.length(), proposal.getImage(), displayString, proposal.getContextInformation(), replacementString));
            }
        }
        
        return filteredProposals;
    }
    
    @Nullable
    public JetSimpleNameExpression getSimpleNameExpression(@NotNull JavaEditor editor, int identOffset) {
        String sourceCode = EditorUtil.getSourceCode(editor);
        String sourceCodeWithMarker = new StringBuilder(sourceCode).insert(identOffset, "KotlinRulezzz").toString();
        
        JetFile jetFile = (JetFile) KotlinPsiManager.INSTANCE.getParsedFile(EditorUtil.getFile(editor), sourceCodeWithMarker);
        
        int offsetWithourCR = LineEndUtil.convertCrToOsOffset(sourceCodeWithMarker, identOffset);
        PsiElement psiElement = jetFile.findElementAt(offsetWithourCR);
        
        return PsiTreeUtil.getParentOfType(psiElement, JetSimpleNameExpression.class);
    }
}
