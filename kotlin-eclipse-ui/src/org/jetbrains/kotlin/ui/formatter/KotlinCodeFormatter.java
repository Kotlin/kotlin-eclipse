package org.jetbrains.kotlin.ui.formatter;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jetbrains.kotlin.parser.KotlinParser;
import org.jetbrains.kotlin.ui.editors.KotlinEditor;

import com.intellij.lang.ASTNode;

public class KotlinCodeFormatter extends AbstractHandler {
    
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        KotlinEditor editor = (KotlinEditor) HandlerUtil.getActiveEditor(event);
        IDocument sourceDocument = editor.getDocumentProvider().getDocument(HandlerUtil.getActiveEditorInput(event)); 
        
        ASTNode parsedFile = KotlinParser.parseText(sourceDocument.get());
        if (parsedFile != null) {
            sourceDocument.set(AlignmentStrategy.alignCode(parsedFile));

        }
        
        return null;
    }

}