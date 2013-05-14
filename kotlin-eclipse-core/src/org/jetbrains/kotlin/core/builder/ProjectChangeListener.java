package org.jetbrains.kotlin.core.builder;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;

public class ProjectChangeListener implements IResourceDeltaVisitor {
    // TODO: Need to reanalize in background for every file change or on manual project rebuild.
    @Override
    public boolean visit(IResourceDelta delta) throws CoreException {
        IResource resource = delta.getResource();
        if (KotlinManager.isCompatibleResource(resource)) {
            KotlinManager.updateProjectPsiSources(resource, delta.getKind());
        }
        
        return true;
    }
}