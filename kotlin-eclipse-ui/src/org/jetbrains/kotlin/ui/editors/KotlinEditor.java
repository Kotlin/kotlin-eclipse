package org.jetbrains.kotlin.ui.editors;

import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.editors.text.TextEditor;

public class KotlinEditor extends TextEditor {

    private final ColorManager colorManager;
    private final BracketInserter bracketInserter;
    
    public KotlinEditor() {
        super();
        colorManager = new ColorManager();
        bracketInserter = new BracketInserter();        
        
        setSourceViewerConfiguration(new Configuration(colorManager));
        setDocumentProvider(new DocumentProvider());
    }

    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);
        
        ISourceViewer sourceViewer = getSourceViewer();
        if (sourceViewer instanceof ITextViewerExtension) {
            bracketInserter.setSourceViewer(sourceViewer);
            bracketInserter.addBrackets('(', ')');
            bracketInserter.addBrackets('"', '"');
            bracketInserter.addBrackets('\'', '\'');
            bracketInserter.addBrackets('[', ']');        
            ((ITextViewerExtension) sourceViewer).prependVerifyKeyListener(bracketInserter);
        }
    }

    @Override
    public void dispose() {
        colorManager.dispose();
        ISourceViewer sourceViewer = getSourceViewer();
        if (sourceViewer instanceof ITextViewerExtension) {
            ((ITextViewerExtension) sourceViewer).removeVerifyKeyListener(bracketInserter);
        }
        
        super.dispose();
    }
}
