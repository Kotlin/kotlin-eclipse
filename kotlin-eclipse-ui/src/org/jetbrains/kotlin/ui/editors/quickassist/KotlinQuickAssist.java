package org.jetbrains.kotlin.ui.editors.quickassist;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil;
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.ui.editors.KotlinEditor;

import com.intellij.psi.PsiElement;

public abstract class KotlinQuickAssist {
    public abstract boolean isApplicable(@NotNull PsiElement psiElement);
    
    public boolean isApplicable() {
        PsiElement element = getActiveElement();
        if (element == null) {
            return false;
        }
        
        return isApplicable(element);
    }
    
    @Nullable
    protected KotlinEditor getActiveEditor() {
        IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

        if (workbenchWindow == null) {
            return null;
        }

        return (KotlinEditor) workbenchWindow.getActivePage().getActiveEditor();
    }
    
    @Nullable
    protected PsiElement getActiveElement() {
        AbstractTextEditor editor = getActiveEditor();
        if (editor == null) return null;

        KotlinEditor javaEditor = (KotlinEditor) editor;
        IFile file = EditorUtil.getFile(javaEditor);
        JetFile jetFile = KotlinPsiManager.INSTANCE.getParsedFile(file);

        int caretOffset = LineEndUtil.convertCrToDocumentOffset(EditorUtil.getDocument(javaEditor), getCaretOffset(javaEditor));

        return jetFile.findElementAt(caretOffset);
    }
    
    protected int getCaretOffset(@NotNull KotlinEditor activeEditor) {
        ISelection selection = activeEditor.getSelectionProvider().getSelection();
        if (selection instanceof ITextSelection) {
            ITextSelection textSelection = (ITextSelection) selection;
            return textSelection.getOffset();
        }

        return activeEditor.getViewer().getTextWidget().getCaretOffset();
    }
}
