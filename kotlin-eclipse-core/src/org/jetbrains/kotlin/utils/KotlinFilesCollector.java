package org.jetbrains.kotlin.utils;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IStartup;
import org.jetbrains.kotlin.builder.KotlinManager;

public class KotlinFilesCollector implements IStartup {

    @Override
    public void earlyStartup() {
        try {
            addFilesToParse();
        } catch (CoreException e) {
            e.printStackTrace();
        }
    }
    
    private void addFilesToParse() throws CoreException {
        IProject[] projects = getWorkspace().getRoot().getProjects();
        for (int i = 0; i < projects.length; ++i) {
            IResource[] resources = projects[i].members(false);
            for (int j = 0; j < resources.length; ++j) {
                scanForFiles(resources[j]);
            }
        }
    }
    
    private void scanForFiles(IResource resource) throws CoreException {
        if (KotlinManager.isCompatibleResource(resource)) {
            KotlinManager.updateProjectPsiSources(resource, IResourceDelta.ADDED);
            return;
        }
        if (resource.getType() != IResource.FOLDER) {
            return;
        }
        IResource[] resources = ((IFolder) resource).members();
        for (int i = 0; i < resources.length; ++i) {
            if (KotlinManager.isCompatibleResource(resources[i])) {
                KotlinManager.updateProjectPsiSources(resources[i], IResourceDelta.ADDED);
            } else if (resources[i].getType() == IResource.FOLDER) {
                scanForFiles(resources[i]);
            }
        }
    }

}
