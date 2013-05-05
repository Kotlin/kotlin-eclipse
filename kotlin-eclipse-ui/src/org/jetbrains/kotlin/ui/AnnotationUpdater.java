package org.jetbrains.kotlin.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.editors.text.TextEditor;
import org.jetbrains.kotlin.ui.editors.AnalyzerScheduler;

public class AnnotationUpdater implements IResourceChangeListener {

    private final AnalyzerScheduler analyzerScheduler;
    
    public AnnotationUpdater(IFile file, TextEditor editor) {
        this.analyzerScheduler = new AnalyzerScheduler(file, editor);
    }

    @Override
    public void resourceChanged(IResourceChangeEvent event) {
        switch (event.getType()) {
            case IResourceChangeEvent.POST_CHANGE:
                try {
                    event.getDelta().accept(new IResourceDeltaVisitor() {
                        
                        @Override
                        public boolean visit(IResourceDelta delta) throws CoreException {
                            while (!(analyzerScheduler.getState() == Job.NONE)) {
                                analyzerScheduler.cancel();
                            }
                            analyzerScheduler.schedule(100);
                            return false;
                        }
                    });
                } catch (CoreException e) {
                    e.printStackTrace();
                }
            break;
    }
    }
}
