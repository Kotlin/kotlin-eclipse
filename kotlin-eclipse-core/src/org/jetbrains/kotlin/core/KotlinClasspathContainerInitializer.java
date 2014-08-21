package org.jetbrains.kotlin.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

public class KotlinClasspathContainerInitializer extends ClasspathContainerInitializer {
    
    private static final IClasspathContainer[] CONTAINERS = new IClasspathContainer[] { new KotlinClasspathContainer() };
    
    @Override
    public void initialize(IPath containerPath, IJavaProject javaProject) throws CoreException {
        if (!(JavaCore.getClasspathContainer(KotlinClasspathContainer.CONTAINER_ID, javaProject) instanceof KotlinClasspathContainer)) {
            JavaCore.setClasspathContainer(containerPath, new IJavaProject[] { javaProject }, CONTAINERS, null);
        }
    }
}
