package org.jetbrains.kotlin.ui.editors;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.editors.text.TextEditor;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer;

import com.intellij.openapi.util.TextRange;

public class AnalyzerScheduler extends Job {

    private final IFile file;
    private final TextEditor editor;
    
    private boolean canceling;
    
    public AnalyzerScheduler(IFile file, TextEditor editor) {
        super("Analyze");
        this.file = file;
        this.editor = editor;
        
        canceling = false;
    }

    @Override
    protected void canceling() {
        canceling = true;
    }
    
    @Override
    protected IStatus run(IProgressMonitor monitor) {
        try {
            if (canceling) {
                return Status.CANCEL_STATUS;
            }
            
            BindingContext bindingContext = KotlinAnalyzer.analyze();
            
            List<Diagnostic> diagnostics = new ArrayList<Diagnostic>(bindingContext.getDiagnostics());
            List<KotlinAnnotation> annotations = new LinkedList<KotlinAnnotation>();
            for (Diagnostic diagnostic : diagnostics) {
                if (!diagnostic.getPsiFile().getVirtualFile().getPath().endsWith(file.getFullPath().toString())) {
                    continue;
                }
                List<TextRange> ranges = diagnostic.getTextRanges();
                if (ranges.isEmpty()) {
                    continue;
                }
                switch (diagnostic.getSeverity()) {
                    case ERROR:
                        annotations.add(new KotlinAnnotation(ranges.get(0).getStartOffset(), 
                                ranges.get(0).getLength(), AnnotationManager.annotationErrorType));
                        break;
                    default:
                        continue;
                }
            }
            
            if (canceling) {
                return Status.CANCEL_STATUS;
            }
            
            AnnotationManager.updateAnnotations(file, editor, annotations);
            
            return Status.OK_STATUS;
            
        } finally {
            canceling = false;
        }
    }

}
