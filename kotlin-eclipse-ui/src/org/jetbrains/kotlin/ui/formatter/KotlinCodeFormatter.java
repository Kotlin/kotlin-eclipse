package org.jetbrains.kotlin.ui.formatter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.kotlin.parser.KotlinParser;
import org.jetbrains.kotlin.ui.editors.KotlinEditor;

import com.intellij.lang.ASTNode;

public class KotlinCodeFormatter extends AbstractHandler {
    
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        KotlinEditor editor = (KotlinEditor) HandlerUtil.getActiveEditor(event);
        String sourceCode = editor.getDocumentProvider().getDocument(HandlerUtil.getActiveEditorInput(event)).get(); 
        
        ASTNode parsedFile = null;
        try {
            File tempFile = File.createTempFile("temp", "." + JetFileType.INSTANCE.getDefaultExtension());
            
            BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile));
            bw.write(sourceCode);
            bw.close();
            
            parsedFile = new KotlinParser(tempFile).parse();
            
            tempFile.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        if (parsedFile != null) {
            editor.getDocumentProvider().getDocument(HandlerUtil.getActiveEditorInput(event)).set(
                    AlignmentStrategy.alignCode(parsedFile));

        }
        
        return null;
    }

}