package org.jetbrains.kotlin.ui.editors.completion;

import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.utils.EditorUtil;
import org.jetbrains.kotlin.utils.LineEndUtil;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;

public class KotlinCompletionUtils {

    public static final KotlinCompletionUtils INSTANCE = new KotlinCompletionUtils();
    
    private KotlinCompletionUtils() {
    }
    
    @NotNull
    public Collection<DeclarationDescriptor> filterCompletionProposals(@NotNull List<DeclarationDescriptor> descriptors, 
            @NotNull final String prefix) {
        return Collections2.filter(descriptors, new Predicate<DeclarationDescriptor>() {
            @Override
            public boolean apply(DeclarationDescriptor descriptor) {
                String identifier = descriptor.getName().getIdentifier();
                return identifier.startsWith(prefix) || 
                       identifier.toLowerCase().startsWith(prefix) || 
                       SearchPattern.camelCaseMatch(prefix, identifier);
            }
        });
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
