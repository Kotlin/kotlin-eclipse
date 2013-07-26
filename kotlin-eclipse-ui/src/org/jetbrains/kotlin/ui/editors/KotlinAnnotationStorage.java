package org.jetbrains.kotlin.ui.editors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.Severity;
import org.jetbrains.jet.lang.diagnostics.rendering.DefaultErrorMessages;
import org.jetbrains.kotlin.utils.LineEndUtil;

import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;

public class KotlinAnnotationStorage {

    public static final KotlinAnnotationStorage INSTANCE = new KotlinAnnotationStorage();
    
    private KotlinAnnotationStorage() {
    }
    
    public Map<IFile, List<DiagnosticAnnotation>> handleDiagnostics(List<Diagnostic> diagnostics) {
        Map<IFile, List<DiagnosticAnnotation>> annotations = new HashMap<IFile, List<DiagnosticAnnotation>>();
        for (Diagnostic diagnostic : diagnostics) {
            if (diagnostic.getTextRanges().isEmpty()) {
                continue;
            }
            
            VirtualFile virtualFile = diagnostic.getPsiFile().getVirtualFile();
            if (virtualFile == null) {
                continue;
            }
            
            IFile curFile = ResourcesPlugin.getWorkspace().getRoot().
                    getFileForLocation(new Path(virtualFile.getPath()));
            
            if (!annotations.containsKey(curFile)) {
                annotations.put(curFile, new ArrayList<DiagnosticAnnotation>());
            }
            
            DiagnosticAnnotation annotation = createKotlinAnnotation(diagnostic);
            annotations.get(curFile).add(annotation);
        }
        
        return annotations;
    }
    
    @NotNull
    private DiagnosticAnnotation createKotlinAnnotation(@NotNull Diagnostic diagnostic) {
        List<TextRange> ranges = diagnostic.getTextRanges();
        String text = diagnostic.getPsiFile().getText();
        
        int offset = LineEndUtil.convertLfToOsOffset(text, ranges.get(0).getStartOffset());
        int length = ranges.get(0).getLength();
        
        String message = DefaultErrorMessages.RENDERER.render(diagnostic);
        String annotationType = getAnnotationType(diagnostic.getSeverity());
        
        DiagnosticAnnotation annotation = new DiagnosticAnnotation(offset, length, annotationType, message);
        
        return annotation;
    }
    
    @Nullable
    private String getAnnotationType(@NotNull Severity severity) {
        String annotationType = null;
        switch (severity) {
            case ERROR:
                annotationType = AnnotationManager.ANNOTATION_ERROR_TYPE;
                break;
            case WARNING:
                annotationType = AnnotationManager.ANNOTATION_WARNING_TYPE;
                break;
            default:
                break;
        }
        
        return annotationType;
    }
    
    public void updateActiveEditorAnnotations(@NotNull final Map<IFile, List<DiagnosticAnnotation>> annotations) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                
                if (workbenchWindow == null) {
                    return;
                }
                
                AbstractTextEditor editor = (AbstractTextEditor) workbenchWindow.getActivePage().getActiveEditor();
                if (editor != null) {
                    updateMarkers(editor, annotations);
                }
            }
        });
    }
    
    private void updateMarkers(@NotNull AbstractTextEditor editor, 
            @NotNull Map<IFile, List<DiagnosticAnnotation>> annotations) {
        IFile file = (IFile) editor.getEditorInput().getAdapter(IFile.class);

        List<DiagnosticAnnotation> newAnnotations = annotations.get(file);
        if (newAnnotations == null) {
            newAnnotations = Collections.emptyList();
        }
        AnnotationManager.updateAnnotations(editor, newAnnotations);
    }
}
