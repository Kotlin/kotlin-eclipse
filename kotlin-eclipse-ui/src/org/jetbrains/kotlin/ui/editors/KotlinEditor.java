package org.jetbrains.kotlin.ui.editors;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.ResourceUtil;

@SuppressWarnings("restriction")
public class KotlinEditor extends CompilationUnitEditor {

    private final ColorManager colorManager;
    private final BracketInserter bracketInserter;
    private AnnotationUpdater annotationUpdater;
    
    public KotlinEditor() {
        super();
        colorManager = new ColorManager();
        bracketInserter = new BracketInserter();        
        
        setSourceViewerConfiguration(new Configuration(colorManager));
    }
    
    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);
        
        ISourceViewer sourceViewer = getSourceViewer();
        if (sourceViewer instanceof ITextViewerExtension) {
            bracketInserter.setSourceViewer(sourceViewer);
            bracketInserter.addBrackets('{', '}');
            ((ITextViewerExtension) sourceViewer).prependVerifyKeyListener(bracketInserter);
            
            annotationUpdater = AnnotationUpdater.INSTANCE;
            
            AnalyzerScheduler.INSTANCE.addFile(ResourceUtil.getFile(this.getEditorInput()), this);
            
            getWorkspace().addResourceChangeListener(annotationUpdater, IResourceChangeEvent.POST_CHANGE);
        }
    }
    
    @Override
    protected void createActions() {
        super.createActions();

        IAction formatAction = new KotlinFormatAction(this);
        formatAction.setText("Format");
        formatAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.FORMAT);
        setAction("Format", formatAction);
        markAsStateDependentAction("Format", true);
        markAsSelectionDependentAction("Format", true);
        PlatformUI.getWorkbench().getHelpSystem().setHelp(formatAction, IJavaHelpContextIds.FORMAT_ACTION);
        
        setAction("QuickFormat", null);
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
