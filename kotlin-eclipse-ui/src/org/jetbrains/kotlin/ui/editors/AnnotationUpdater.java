package org.jetbrains.kotlin.ui.editors;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.Job;
import org.jetbrains.kotlin.core.builder.KotlinManager;

public class AnnotationUpdater implements IResourceChangeListener {
    
    public static final AnnotationUpdater INSTANCE = new AnnotationUpdater();

    private final AnalyzerScheduler analyzerScheduler;
    
    private AnnotationUpdater() {
        analyzerScheduler = AnalyzerScheduler.INSTANCE;
    }

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
                        if (analyzerScheduler.getState() != Job.NONE) {
                            analyzerScheduler.cancel();
                        }
                        analyzerScheduler.schedule();
                        
                        return true;
                    }
                });
            } catch (CoreException e) {
                e.printStackTrace();
            }
        }
    }
}