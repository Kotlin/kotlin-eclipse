package org.jetbrains.kotlin.ui.debug;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTarget;
import org.eclipse.jdt.debug.core.IJavaLineBreakpoint;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil;
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.JetClass;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.psi.JetPsiUtil;

import com.intellij.psi.PsiElement;

public class KotlinToggleBreakpointAdapter implements IToggleBreakpointsTarget {
    @Override
    public void toggleLineBreakpoints(IWorkbenchPart part, ISelection selection) throws CoreException {
        ITextEditor editor = getEditor(part);
        if (editor == null) {
            return;
        }

        IFile file = EditorUtil.getFile(editor);
        if (file == null) {
            KotlinLogger.logError("Failed to retrieve IFile from editor " + editor, null);
            return;
        }

        int lineNumber = ((ITextSelection) selection).getStartLine() + 1;
        IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
        String typeName = getTypeName(document, lineNumber, file);

        IJavaLineBreakpoint existingBreakpoint = JDIDebugModel.lineBreakpointExists(file, typeName, lineNumber);
        if (existingBreakpoint != null) {
            existingBreakpoint.delete();
        } else {
            JDIDebugModel.createLineBreakpoint(file, typeName, lineNumber, -1, -1, 0, true, null);
        }
    }
    
    @Override
    public boolean canToggleLineBreakpoints(IWorkbenchPart part, ISelection selection) {
        return true;
    }
    
    @Override
    public void toggleMethodBreakpoints(IWorkbenchPart part, ISelection selection) throws CoreException {
    }
    
    @Override
    public boolean canToggleMethodBreakpoints(IWorkbenchPart part, ISelection selection) {
        return true;
    }
    
    @Override
    public void toggleWatchpoints(IWorkbenchPart part, ISelection selection) throws CoreException {
    }
    
    @Override
    public boolean canToggleWatchpoints(IWorkbenchPart part, ISelection selection) {
        return true;
    }
    
    @NotNull
    private String getTypeName(@NotNull IDocument document, int lineNumber, @NotNull IFile file) {
        JetFile kotlinParsedFile = KotlinPsiManager.getKotlinParsedFile(file);
        assert kotlinParsedFile != null;
        
        String typeName = null;
        try {
            typeName = findTopmostType(document.getLineOffset(lineNumber - 1), kotlinParsedFile).asString();
        } catch (BadLocationException e) {
            KotlinLogger.logAndThrow(e);
        }
        
        assert typeName != null;
        return typeName;
    }
    
    @NotNull
    private FqName findTopmostType(int offset, @NotNull JetFile jetFile) {
        PsiElement element = jetFile.findElementAt(offset);
        @SuppressWarnings("unchecked") JetClass jetClass = (JetClass) JetPsiUtil.getTopmostParentOfTypes(element, JetClass.class);
        if (jetClass != null) {
            FqName fqName = jetClass.getFqName();
            if (fqName != null) { // For example, fqName might be null if jetClass is a local class
                return fqName;
            }
        } 
        
        return PackageClassUtils.getPackageClassFqName(jetFile.getPackageFqName());
    }
    
    @Nullable
    private ITextEditor getEditor(@NotNull IWorkbenchPart part) {
        if (part instanceof ITextEditor) {
            return (ITextEditor) part;
        }
        
        return (ITextEditor) part.getAdapter(ITextEditor.class);
    }
}