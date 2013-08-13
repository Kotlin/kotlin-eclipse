package org.jetbrains.kotlin.utils;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.ui.texteditor.AbstractTextEditor;

public class EditorUtil {
    
    public static IFile getFile(AbstractTextEditor editor) {
        return (IFile) editor.getEditorInput().getAdapter(IFile.class);
    }
    
    public static String getSourceCode(JavaEditor editor) {
        return editor.getViewer().getDocument().get();
    }
}