package org.jetbrains.kotlin.ui.editors;

import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.ui.builder.KotlinBuilder;
import org.jetbrains.kotlin.ui.editors.outline.KotlinOutlinePage;
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
        
        updateLineAnnotations(file.getProject());
        updateActiveOutlinePage();
    }
    
    private void updateLineAnnotations(final IProject project) {
        List<Diagnostic> diagnostics = KotlinBuilder.analyzeProjectInForeground(JavaCore.create(project));
        Map<IFile, List<DiagnosticAnnotation>> annotations = DiagnosticAnnotationUtil.INSTANCE.handleDiagnostics(diagnostics);
        DiagnosticAnnotationUtil.INSTANCE.updateActiveEditorAnnotations(annotations);
    }
    
    private void updateActiveOutlinePage() {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                
                if (workbenchWindow == null) {
                    return;
                }
                
                AbstractTextEditor editor = (AbstractTextEditor) workbenchWindow.getActivePage().getActiveEditor();
                if (editor != null) {
                    IContentOutlinePage outlinePage = (IContentOutlinePage) editor.getAdapter(IContentOutlinePage.class);
                    if (outlinePage instanceof KotlinOutlinePage) {
                        ((KotlinOutlinePage) outlinePage).refresh();
                    }
                }
            }
        });
    }
}
