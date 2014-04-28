package org.jetbrains.kotlin.ui.editors.quickassist;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.ui.editors.KotlinEditor;
import org.jetbrains.kotlin.utils.EditorUtil;
import org.jetbrains.kotlin.utils.LineEndUtil;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

public abstract class KotlinQuickAssistProposal implements IJavaCompletionProposal {

    @Override
    public void apply(IDocument document) {
        PsiElement element = getActiveElement();
        if (element != null) {
            apply(document, element);
        }
    }
    
    public abstract void apply(@NotNull IDocument document, @NotNull PsiElement psiElement);
    
    public boolean isApplicable() {
        PsiElement element = getActiveElement();
        if (element == null) {
            return false;
        }
        
        return isApplicable(element);
    }
    
    @Nullable
    protected static PsiElement getActiveElement() {
        AbstractTextEditor editor = getActiveEditor();
        if (editor == null) return null;

        JavaEditor javaEditor = (JavaEditor) editor;
        IFile file = EditorUtil.getFile(javaEditor);
        JetFile jetFile = (JetFile) KotlinPsiManager.INSTANCE.getParsedFile(file);

        int caretOffset = LineEndUtil.convertCrToOsOffset(EditorUtil.getSourceCode(javaEditor), getCaretOffset(javaEditor));

        return jetFile.findElementAt(caretOffset);
    }
    
    @Nullable
    protected static KotlinEditor getActiveEditor() {
        IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

        if (workbenchWindow == null) {
            return null;
        }

        return (KotlinEditor) workbenchWindow.getActivePage().getActiveEditor();
    }
    
    private static int getCaretOffset(@NotNull JavaEditor activeEditor) {
        ISelection selection = activeEditor.getSelectionProvider().getSelection();
        if (selection instanceof ITextSelection) {
            ITextSelection textSelection = (ITextSelection) selection;
            return textSelection.getOffset();
        }

        return activeEditor.getViewer().getTextWidget().getCaretOffset();
    }
    
    public int getStartOffset(@NotNull PsiElement element, @NotNull AbstractTextEditor editor) {
        PsiFile parsedFile = KotlinPsiManager.INSTANCE.getParsedFile(EditorUtil.getFile(editor));
        return LineEndUtil.convertLfToOsOffset(parsedFile.getText(), element.getTextRange().getStartOffset());
    }
    
    public int getEndOffset(@NotNull PsiElement element, @NotNull AbstractTextEditor editor) {
        PsiFile parsedFile = KotlinPsiManager.INSTANCE.getParsedFile(EditorUtil.getFile(editor));
        return LineEndUtil.convertLfToOsOffset(parsedFile.getText(), element.getTextRange().getEndOffset());
    }
    
    public abstract boolean isApplicable(@NotNull PsiElement psiElement);

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