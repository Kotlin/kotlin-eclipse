package org.jetbrains.kotlin.ui;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.jetbrains.kotlin.ui.editors.AnalyzerScheduler;

public class AnnotationUpdater implements IResourceChangeListener {
    
    public static final AnnotationUpdater INSTANCE = new AnnotationUpdater();

    private final AnalyzerScheduler analyzerScheduler;
    
    private AnnotationUpdater() {
        analyzerScheduler = AnalyzerScheduler.INSTANCE;
    }

    @Override
    public void resourceChanged(IResourceChangeEvent event) {
        if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
            if (analyzerScheduler.getState() != Job.NONE) {
                analyzerScheduler.cancel();
            }
            analyzerScheduler.schedule();
        }
    }
}
