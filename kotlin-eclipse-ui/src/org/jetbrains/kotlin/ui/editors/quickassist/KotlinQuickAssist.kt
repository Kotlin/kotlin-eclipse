package org.jetbrains.kotlin.ui.editors.quickassist;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory;
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil;
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor;
import org.jetbrains.kotlin.ui.editors.annotations.DiagnosticAnnotation;
import org.jetbrains.kotlin.ui.editors.annotations.DiagnosticAnnotationUtil;

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
    protected KotlinFileEditor getActiveEditor() {
        IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

        if (workbenchWindow == null) {
            return null;
        }

        return (KotlinFileEditor) workbenchWindow.getActivePage().getActiveEditor();
    }
    
    @Nullable
    protected PsiElement getActiveElement() {
        AbstractTextEditor editor = getActiveEditor();
        if (editor == null) return null;

        KotlinFileEditor kotlinEditor = (KotlinFileEditor) editor;
        IFile file = kotlinEditor.getFile();
        
        if (file != null) {
            IDocument document = kotlinEditor.getDocument();
            KtFile jetFile = KotlinPsiManager.getKotlinFileIfExist(file, document.get());
            if (jetFile == null) return null;
            
            int caretOffset = LineEndUtil.convertCrToDocumentOffset(document, getCaretOffset(kotlinEditor));
            return jetFile.findElementAt(caretOffset);
        } else {
            KotlinLogger.logError("Failed to retrieve IFile from editor " + editor, null);
        }

        return null;
    }
    
    protected int getCaretOffset(@NotNull KotlinFileEditor activeEditor) {
        ISelection selection = activeEditor.getSelectionProvider().getSelection();
        if (selection instanceof ITextSelection) {
            ITextSelection textSelection = (ITextSelection) selection;
            return textSelection.getOffset();
        }
        
        return activeEditor.getViewer().getTextWidget().getCaretOffset();
    }
    
    protected int getCaretOffsetInPSI(@NotNull KotlinFileEditor activeEditor, IDocument document) {
        int caretOffset = getCaretOffset(activeEditor);
        return LineEndUtil.convertCrToDocumentOffset(document, caretOffset);
    }
    
    
    public boolean isDiagnosticActiveForElement(PsiElement element, @NotNull DiagnosticFactory<?> diagnosticType, @NotNull String attribute) {
        KotlinFileEditor editor = getActiveEditor();
        if (editor == null) {
            return false;
        }
        
        int caretOffset = getCaretOffset(editor);
        DiagnosticAnnotation annotation = DiagnosticAnnotationUtil.INSTANCE.getAnnotationByOffset(editor, caretOffset);
        if (annotation != null) {
            DiagnosticFactory<?> diagnostic = annotation.getDiagnostic();
            return diagnostic != null ? diagnostic.equals(diagnosticType) : false;
        }
        
        IFile file = EditorUtil.getFile(editor);
        if (file == null) {
            KotlinLogger.logError("Failed to retrieve IFile from editor " + editor, null);
            return false;
        }
        
        IMarker marker = DiagnosticAnnotationUtil.INSTANCE.getMarkerByOffset(file, caretOffset);
        return marker != null ? marker.getAttribute(attribute, false) : false;
    }
}
