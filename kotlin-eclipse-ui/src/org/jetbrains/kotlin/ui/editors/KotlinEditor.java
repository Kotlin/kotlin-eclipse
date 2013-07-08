package org.jetbrains.kotlin.ui.editors;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.ide.ResourceUtil;

public class KotlinEditor extends TextEditor {

    private final ColorManager colorManager;
    private final BracketInserter bracketInserter;
    private AnnotationUpdater annotationUpdater;
    
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
            bracketInserter.addBrackets('{', '}');
            ((ITextViewerExtension) sourceViewer).prependVerifyKeyListener(bracketInserter);
            
            annotationUpdater = AnnotationUpdater.INSTANCE;
            
            AnalyzerScheduler.INSTANCE.addFile(ResourceUtil.getFile(this.getEditorInput()), this);
            
            getWorkspace().addResourceChangeListener(annotationUpdater, IResourceChangeEvent.POST_CHANGE);
        }
    }
    
    public ISourceViewer getViewer() {
        return super.getSourceViewer();
    }
    
    @Override
    public void dispose() {
        if (annotationUpdater != null) {
            getWorkspace().removeResourceChangeListener(annotationUpdater);
        }
        AnalyzerScheduler.INSTANCE.excludeFile(ResourceUtil.getFile(this.getEditorInput()));

        colorManager.dispose();
        ISourceViewer sourceViewer = getSourceViewer();
        if (sourceViewer instanceof ITextViewerExtension) {
            ((ITextViewerExtension) sourceViewer).removeVerifyKeyListener(bracketInserter);
        }
        
        super.dispose();
    }
}
