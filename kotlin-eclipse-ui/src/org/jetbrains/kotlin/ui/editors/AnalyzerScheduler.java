package org.jetbrains.kotlin.ui.editors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.editors.text.TextEditor;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.rendering.DefaultErrorMessages;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.kotlin.core.builder.KotlinManager;
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
                
                List<IFile> workspaceFiles = new ArrayList<IFile>(KotlinManager.getAllFiles());
                for (IFile file : workspaceFiles) {
                    try {
                        file.deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
                    } catch (CoreException e) {
                    }
                }
                
                List<Diagnostic> diagnostics = new ArrayList<Diagnostic>(bindingContext.getDiagnostics());
                Map<IFile, List<KotlinAnnotation>> annotations = new HashMap<IFile, List<KotlinAnnotation>>();
                for (Diagnostic diagnostic : diagnostics) {
                    List<TextRange> ranges = diagnostic.getTextRanges();
                    if (ranges.isEmpty()) {
                        continue;
                    }
                    String annotationType = null;
                    int problemSeverity = 0;
                    switch (diagnostic.getSeverity()) {
                        case ERROR:
                            annotationType = AnnotationManager.annotationErrorType;
                            problemSeverity = IMarker.SEVERITY_ERROR;
                            break;
                        case WARNING:
                            annotationType = AnnotationManager.annotationWarningType;
                            problemSeverity = IMarker.SEVERITY_WARNING;
                            break;
                        default:
                            continue;
                    }
                    
                    IFile curFile = ResourcesPlugin.getWorkspace().getRoot().
                            getFileForLocation(new Path(diagnostic.getPsiFile().getVirtualFile().getPath()));
                    try {
                        IMarker problemMarker = curFile.createMarker(IMarker.PROBLEM);
                        problemMarker.setAttribute(IMarker.MESSAGE, DefaultErrorMessages.RENDERER.render(diagnostic));
                        problemMarker.setAttribute(IMarker.SEVERITY, problemSeverity);
                        problemMarker.setAttribute(IMarker.CHAR_START, ranges.get(0).getStartOffset());
                        problemMarker.setAttribute(IMarker.CHAR_END, ranges.get(0).getEndOffset());
                    } catch (Exception e) {
                    }
                    
                    if (!fileSet.contains(curFile)) {
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
