package org.jetbrains.kotlin.ui.editors;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.jetbrains.kotlin.core.builder.KotlinManager;
import org.jetbrains.kotlin.core.log.KotlinLogger;

public class AnnotationUpdater implements IResourceChangeListener {
    
    public static final AnnotationUpdater INSTANCE = new AnnotationUpdater();

    @Override
    public void resourceChanged(IResourceChangeEvent event) {
        if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
            try {
                event.getDelta().accept(new IResourceDeltaVisitor() {
                    
                    @Override
                    public boolean visit(IResourceDelta delta) throws CoreException {
                        if (!KotlinManager.isProjectChangedState(delta)) {
                            return true;
                        }
                        
                        IJavaProject javaProject = JavaCore.create(delta.getResource().getProject());
                        AnalyzerScheduler.analyzeProjectInBackground(javaProject);
                        
                        return true;
                    }
                });
            } catch (CoreException e) {
                KotlinLogger.logError(e);
            }
        }
    }
}