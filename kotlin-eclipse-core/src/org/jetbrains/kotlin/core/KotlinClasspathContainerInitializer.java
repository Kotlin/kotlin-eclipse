package org.jetbrains.kotlin.core;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.core.filesystem.KotlinFileSystem;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.model.KotlinJavaManager;

public class KotlinClasspathContainerInitializer extends ClasspathContainerInitializer {
    
    @Override
    public void initialize(IPath containerPath, IJavaProject javaProject) throws CoreException {
        if (!KotlinJavaManager.INSTANCE.hasLinkedKotlinBinFolder(javaProject)) {
            addFolderForKotlinClassFiles(javaProject);
        }
        
        IClasspathContainer[] CONTAINERS = new IClasspathContainer[] { new KotlinClasspathContainer(javaProject) };
        if (!(JavaCore.getClasspathContainer(KotlinClasspathContainer.CONTAINER_ID, javaProject) instanceof KotlinClasspathContainer)) {
            JavaCore.setClasspathContainer(containerPath, new IJavaProject[] { javaProject }, CONTAINERS, null);
        }
    }
    
    private URI setKotlinFileSystemScheme(@NotNull IFolder folder) {
        URI locationURI = folder.getLocationURI();
        try {
            IPath path = new Path(folder.getProject().getName()).append(KotlinJavaManager.KOTLIN_BIN_FOLDER).makeAbsolute();
            return new URI(
                    KotlinFileSystem.SCHEME, 
                    locationURI.getUserInfo(), 
                    locationURI.getHost(), 
                    locationURI.getPort(), 
                    path.toPortableString(), 
                    locationURI.getQuery(), 
                    locationURI.getFragment());
        } catch (URISyntaxException e) {
            KotlinLogger.logAndThrow(e);
            throw new IllegalStateException(e);
        }
    }
    
    private void addFolderForKotlinClassFiles(@NotNull IJavaProject javaProject) throws CoreException { 
        IFolder folder = javaProject.getProject().getFolder(KotlinJavaManager.KOTLIN_BIN_FOLDER);
        folder.createLink(setKotlinFileSystemScheme(folder), IResource.REPLACE | IResource.ALLOW_MISSING_LOCAL, null);
    }
}
