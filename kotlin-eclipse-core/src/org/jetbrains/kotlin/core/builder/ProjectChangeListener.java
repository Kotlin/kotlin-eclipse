package org.jetbrains.kotlin.core.builder;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;

public class ProjectChangeListener implements IResourceDeltaVisitor {

    @Override
    public boolean visit(IResourceDelta delta) throws CoreException {
        IResource resource = delta.getResource();
        if (KotlinPsiManager.INSTANCE.isCompatibleResource(resource)) {
            KotlinPsiManager.INSTANCE.updateProjectPsiSources((IFile) resource, delta.getKind());
        }
        
        return true;
    }
}