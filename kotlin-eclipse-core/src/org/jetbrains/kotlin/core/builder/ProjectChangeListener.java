package org.jetbrains.kotlin.core.builder;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;

public class ProjectChangeListener implements IResourceDeltaVisitor {

    @Override
    public boolean visit(IResourceDelta delta) throws CoreException {
        IResource resource = delta.getResource();
        if (resource instanceof IFile) {
            IFile file = (IFile) resource;
            if (KotlinPsiManager.INSTANCE.isCompatibleResource(file)) {
                KotlinPsiManager.INSTANCE.updateProjectPsiSources(file, delta.getKind());
            }
        } else if (resource instanceof IProject) {
            KotlinPsiManager.INSTANCE.updateProjectPsiSources((IProject) resource, delta.getKind());
        }
        
        return true;
    }
}