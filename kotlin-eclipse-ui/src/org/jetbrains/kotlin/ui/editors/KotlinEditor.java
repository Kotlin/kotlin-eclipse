package org.jetbrains.kotlin.ui.editors;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

public class KotlinEditor extends CompilationUnitEditor {

    private final ColorManager colorManager;
    private final BracketInserter bracketInserter;
    
    public KotlinEditor() {
        super();
        colorManager = new ColorManager();
        bracketInserter = new BracketInserter();
    }
    
    @Override
    public void createPartControl(Composite parent) {
        setSourceViewerConfiguration(new Configuration(colorManager, this, getPreferenceStore()));
        
        super.createPartControl(parent);
        
        ISourceViewer sourceViewer = getSourceViewer();
        if (sourceViewer instanceof ITextViewerExtension) {
            bracketInserter.setSourceViewer(sourceViewer);
            bracketInserter.addBrackets('{', '}');
            ((ITextViewerExtension) sourceViewer).prependVerifyKeyListener(bracketInserter);
        }
    }
    
    @Override
    protected void createActions() {
        super.createActions();

        setAction("QuickFormat", null);
        
        IAction formatAction = new KotlinFormatAction(this);
        formatAction.setText("Format");
        formatAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.FORMAT);
        setAction("Format", formatAction);
        markAsStateDependentAction("Format", true);
        markAsSelectionDependentAction("Format", true);
        PlatformUI.getWorkbench().getHelpSystem().setHelp(formatAction, IJavaHelpContextIds.FORMAT_ACTION);
        
        IAction openDeclarationAction = new OpenDeclarationAction(this);
        openDeclarationAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_EDITOR);
        openDeclarationAction.setText(ActionMessages.OpenAction_declaration_label);
        setAction("OpenEditor", openDeclarationAction);
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
