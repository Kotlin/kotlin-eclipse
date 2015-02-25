package org.jetbrains.kotlin.ui.editors.quickassist;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil;
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

public abstract class KotlinQuickAssistProposal extends KotlinQuickAssist implements IJavaCompletionProposal {

    @Override
    public void apply(IDocument document) {
        PsiElement element = getActiveElement();
        if (element != null) {
            apply(document, element);
        }
    }
    
    public abstract void apply(@NotNull IDocument document, @NotNull PsiElement psiElement);
    
    @Nullable
    public IFile getActiveFile() {
        AbstractTextEditor editor = getActiveEditor();
        if (editor == null) return null;
        
        JavaEditor javaEditor = (JavaEditor) editor;
        return EditorUtil.getFile(javaEditor);
    }
    
    public int getStartOffset(@NotNull PsiElement element, @NotNull AbstractTextEditor editor) {
        PsiFile parsedFile = KotlinPsiManager.INSTANCE.getParsedFile(EditorUtil.getFile(editor));
        return LineEndUtil.convertLfToDocumentOffset(parsedFile.getText(), 
                element.getTextRange().getStartOffset(), EditorUtil.getDocument(editor));
    }
    
    public int getEndOffset(@NotNull PsiElement element, @NotNull AbstractTextEditor editor) {
        PsiFile parsedFile = KotlinPsiManager.INSTANCE.getParsedFile(EditorUtil.getFile(editor));
        return LineEndUtil.convertLfToDocumentOffset(parsedFile.getText(), 
                element.getTextRange().getEndOffset(), EditorUtil.getDocument(editor));
    }
    
    @Override
    public abstract String getDisplayString();
    
    @Override
    public Point getSelection(IDocument document) {
        return null;
    }

    @Override
    public String getAdditionalProposalInfo() {
        return null;
    }

    @Override
    public Image getImage() {
        return JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
    }

    @Override
    public IContextInformation getContextInformation() {
        return null;
    }

    @Override
    public int getRelevance() {
        return 0;
    }
}