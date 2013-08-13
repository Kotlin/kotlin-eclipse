package org.jetbrains.kotlin.ui.editors;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.utils.EditorUtil;

public class KotlinReconcilingStrategy implements IReconcilingStrategy {

    private final JavaEditor editor;
    
    public KotlinReconcilingStrategy(JavaEditor editor) {
        this.editor = editor;
    }
    
    @Override
    public void setDocument(IDocument document) {

    }

    @Override
    public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
    }

    @Override
    public void reconcile(IRegion partition) {
        String sourceCode = EditorUtil.getSourceCode(editor);
        IFile file = EditorUtil.getFile(editor);
        
        KotlinPsiManager.INSTANCE.updatePsiFile(file, sourceCode);
    }

}
