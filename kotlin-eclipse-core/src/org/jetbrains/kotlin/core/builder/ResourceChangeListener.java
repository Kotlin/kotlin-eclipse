package org.jetbrains.kotlin.core.builder;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.runtime.CoreException;
import org.jetbrains.kotlin.core.log.KotlinLogger;

public class ResourceChangeListener implements IResourceChangeListener {

    @Override
    public void resourceChanged(IResourceChangeEvent event) {
        switch (event.getType()) {
            case IResourceChangeEvent.POST_CHANGE:
                try {
                    event.getDelta().accept(new ProjectChangeListener());
                } catch (CoreException e) {
                    KotlinLogger.logError(e);
                }
                break;
        }
    }

}