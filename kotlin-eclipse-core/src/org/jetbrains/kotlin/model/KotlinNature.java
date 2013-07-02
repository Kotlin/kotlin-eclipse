package org.jetbrains.kotlin.model;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.jetbrains.kotlin.core.log.KotlinLogger;

public class KotlinNature implements IProjectNature {
    private IProject project;
    public static final String KOTLIN_NATURE = "org.jetbrains.kotlin.core.kotlinNature";
    
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

    public static boolean hasKotlinNature(final IProject project) {
        try {
            return project.hasNature(KOTLIN_NATURE);
        } catch (CoreException e) {
            KotlinLogger.logError("project does not exist or is not open", e);
        }
        return false;
    }
    
    public static void addNature(final IProject project) throws CoreException {
        if (project == null) {
            return;
        }
        
        if (!KotlinNature.hasKotlinNature(project)) {           
            final IProjectDescription description = project.getDescription();
            final String[] ids = description.getNatureIds();
            final String[] newIds = new String[ids == null ? 1 : ids.length + 1];
            if (ids != null) {
                System.arraycopy(ids, 0, newIds, 0, ids.length);                
                newIds[newIds.length - 1] = KotlinNature.KOTLIN_NATURE;
            }               
            description.setNatureIds(newIds);
            project.setDescription(description, null);
        }        
    }
}