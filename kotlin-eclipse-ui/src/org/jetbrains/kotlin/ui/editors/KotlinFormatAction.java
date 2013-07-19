package org.jetbrains.kotlin.ui.editors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.IDocument;
import org.jetbrains.kotlin.parser.KotlinParser;
import org.jetbrains.kotlin.ui.formatter.AlignmentStrategy;

import com.intellij.lang.ASTNode;

public class KotlinFormatAction extends Action {

    private final KotlinEditor editor; 
    
    protected KotlinFormatAction(KotlinEditor editor) {
        this.editor = editor;
    }
    
    @SuppressWarnings("restriction")
    @Override
    public void run() {
        IDocument document = editor.getViewer().getDocument(); 
        String sourceCode = document.get();
        ASTNode parsedCode = KotlinParser.parseText(sourceCode);
        
        document.set(AlignmentStrategy.alignCode(parsedCode));
    }
}
