package org.jetbrains.kotlin.ui.editors.quickassist;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.resolve.AnalysisResultWithProvider;
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer;
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil;
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor;

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
        int offset = element.getTextRange().getStartOffset();
        IFile file = EditorUtil.getFile(editor);

        assert file != null : "Failed to retrieve IFile from editor " + editor;

        PsiFile parsedFile = KotlinPsiManager.INSTANCE.getParsedFile(file);
        return LineEndUtil.convertLfToDocumentOffset(parsedFile.getText(), offset, EditorUtil.getDocument(editor));
    }
    
    public int getEndOffset(@NotNull PsiElement element, @NotNull AbstractTextEditor editor) {
        int offset = element.getTextRange().getEndOffset();
        IFile file = EditorUtil.getFile(editor);

        assert file != null : "Failed to retrieve IFile from editor " + editor;

        PsiFile parsedFile = KotlinPsiManager.INSTANCE.getParsedFile(file);
        return LineEndUtil.convertLfToDocumentOffset(parsedFile.getText(), offset, EditorUtil.getDocument(editor));
    }
    
    public void insertAfter(@NotNull PsiElement element, @NotNull String text) {
        KotlinFileEditor kotlinFileEditor = getActiveEditor();
        assert kotlinFileEditor != null : "Active editor cannot be null";
        
        try {
            kotlinFileEditor.getViewer().getDocument().replace(getEndOffset(element, kotlinFileEditor), 0, text);
        } catch (BadLocationException e) {
            KotlinLogger.logAndThrow(e);
        }
    }
    
    public void replaceBetween(@NotNull PsiElement from, @NotNull PsiElement till, @NotNull String text) {
        KotlinFileEditor kotlinFileEditor = getActiveEditor();
        assert kotlinFileEditor != null : "Active editor cannot be null";
        
        try {
            int startOffset = getStartOffset(from, kotlinFileEditor);
            int endOffset = getEndOffset(till, kotlinFileEditor);
            kotlinFileEditor.getViewer().getDocument().replace(startOffset, endOffset - startOffset, text);
        } catch (BadLocationException e) {
            KotlinLogger.logAndThrow(e);
        }
    }
    
    public void replace(@NotNull PsiElement toReplace, @NotNull String text) {
        replaceBetween(toReplace, toReplace, text);
    }
    
    @Nullable
    protected BindingContext getBindingContext(@NotNull JetFile jetFile)  {
        return getAnalysisResultWithProvider(jetFile).getAnalysisResult().getBindingContext();
    }
    
    protected AnalysisResultWithProvider getAnalysisResultWithProvider(@NotNull JetFile jetFile) {
        IFile file = getActiveFile();
        if (file == null) {
            return null;
        }
        IJavaProject javaProject = JavaCore.create(file.getProject());
        return KotlinAnalyzer.analyzeFile(javaProject, jetFile);
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