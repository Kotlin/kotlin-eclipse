package org.jetbrains.kotlin.model;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

public class KotlinNature implements IProjectNature {
    private IProject project;
    public static final String KOTLIN_NATURE = "org.jetbrains.kotlin.ui.kotlinNature";
    
    @Override
    public void configure() throws CoreException {
        
    }
    
    @Override
    public void deconfigure() throws CoreException {
        
    }
    
    @Override
    public IProject getProject() {
        return this.project;
    }
    
    @Override
    public void setProject(IProject value) {
        this.project = value;
        
    }

    public static boolean hasKotlinNature(IProject project) {
        try {
            return project.hasNature(KOTLIN_NATURE);
        } catch (CoreException e) {
         // project does not exist or is not open
        }
        return false;
    }
}