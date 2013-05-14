package org.jetbrains.kotlin.ui.editors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.editors.text.TextEditor;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer;

import com.intellij.openapi.util.TextRange;

public class AnalyzerScheduler extends Job {
    
    public static final AnalyzerScheduler INSTANCE = new AnalyzerScheduler();
    
    private static Set<IFile> fileSet = new HashSet<IFile>();
    private static Map<IFile, TextEditor> editorMap = new HashMap<IFile, TextEditor>();
    
    private boolean canceling;
    
    private AnalyzerScheduler() {
        super("Analyze");
        
        canceling = false;
    }
    
    public static void addFile(IFile file, TextEditor editor) {
        synchronized(IFile.class) {
            fileSet.add(file);
            editorMap.put(file, editor);
        }
    }
    
    public static void excludeFile(IFile file) {
        synchronized(IFile.class) {
            fileSet.remove(file);
            editorMap.remove(file);
        }
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
            
            synchronized (IFile.class) {
                if (fileSet == null || editorMap == null) {
                    return Status.OK_STATUS;
                }
                
                BindingContext bindingContext = KotlinAnalyzer.analyze();
                
                List<Diagnostic> diagnostics = new ArrayList<Diagnostic>(bindingContext.getDiagnostics());
                Map<IFile, List<KotlinAnnotation>> annotations = new HashMap<IFile, List<KotlinAnnotation>>();
                for (Diagnostic diagnostic : diagnostics) {
                    IFile curFile = ResourcesPlugin.getWorkspace().getRoot().
                            getFileForLocation(new Path(diagnostic.getPsiFile().getVirtualFile().getPath()));
                    if (!fileSet.contains(curFile)) {
                        continue;
                    }
                    List<TextRange> ranges = diagnostic.getTextRanges();
                    if (ranges.isEmpty()) {
                        continue;
                    }
                    String annotationType = null;
                    switch (diagnostic.getSeverity()) {
                        case ERROR:
                            annotationType = AnnotationManager.annotationErrorType;
                            break;
                        case WARNING:
                            annotationType = AnnotationManager.annotationWarningType;
                            break;
                        default:
                            continue;
                    }
                    if (annotations.get(curFile) == null) {
                        annotations.put(curFile, new LinkedList<KotlinAnnotation>());
                    }
                    List<KotlinAnnotation> annotationsList = annotations.get(curFile);
                    annotationsList.add(new KotlinAnnotation(ranges.get(0).getStartOffset(), 
                            ranges.get(0).getLength(), annotationType));
                }
                
                if (canceling) {
                    return Status.CANCEL_STATUS;
                }
                
                for (IFile file : fileSet) {
                    AnnotationManager.updateAnnotations(editorMap.get(file), annotations.get(file));
                }
            }
            
            return Status.OK_STATUS;
            
        } finally {
            canceling = false;
        }
    }

}
