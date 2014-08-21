package org.jetbrains.kotlin.core;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.core.log.KotlinLogger;

public class KotlinClasspathContainerInitializer extends ClasspathContainerInitializer {
    
    public static final KotlinClasspathContainerInitializer INSTANCE = new KotlinClasspathContainerInitializer();
    
    private static final IClasspathContainer[] CONTAINERS = new IClasspathContainer[] { new KotlinClasspathContainer() };
    
    private KotlinClasspathContainerInitializer() {
    }
    
    @Override
    public void initialize(IPath containerPath, IJavaProject javaProject) throws CoreException {
        if (!(JavaCore.getClasspathContainer(KotlinClasspathContainer.CONTAINER_ID, javaProject) instanceof KotlinClasspathContainer)) {
            JavaCore.setClasspathContainer(containerPath, new IJavaProject[] { javaProject }, CONTAINERS, null);
        }
    }
    
    public void initialize(@NotNull IJavaProject javaProject) throws CoreException {
        initialize(KotlinClasspathContainer.CONTAINER_ID, javaProject);
    }
    
    public void initialize(@NotNull IProject project) {
        try {
            initialize(JavaCore.create(project));
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
    }
}
