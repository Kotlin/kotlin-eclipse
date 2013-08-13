package org.jetbrains.kotlin.ui.editors;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.IDocument;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.ui.formatter.AlignmentStrategy;
import org.jetbrains.kotlin.utils.EditorUtil;

import com.intellij.lang.ASTNode;

public class KotlinFormatAction extends Action {

    private final KotlinEditor editor; 
    
    protected KotlinFormatAction(KotlinEditor editor) {
        this.editor = editor;
    }
    
    @Override
    public void run() {
        String sourceCode = EditorUtil.getSourceCode(editor);
        IFile file = EditorUtil.getFile(editor);
        
        ASTNode parsedCode = KotlinPsiManager.INSTANCE.getParsedFile(file, sourceCode);
        
        IDocument document = editor.getViewer().getDocument(); 
        document.set(AlignmentStrategy.alignCode(parsedCode));
    }
}
