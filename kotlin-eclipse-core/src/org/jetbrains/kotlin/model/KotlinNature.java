package org.jetbrains.kotlin.model;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.core.JavaProject;

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
         // project does not exist or is not open
        }
        return false;
    }

    /**
     * Add java nature and Kotlin nature. If project is null then ignore and do nothing
     * @param project project in which we should add natures
     * @throws CoreException if can't add nature.
     */
    public static void addNature(final IProject project) throws CoreException {
        if (project == null) {
            return;
        }

        if(!JavaProject.hasJavaNature(project)){
            addJavaNature(project);
        }

        if (!KotlinNature.hasKotlinNature(project)) {
            addKotlinNature(project);
        }
    }

    private static void addJavaNature(IProject project) throws CoreException {
        addNature(project, JavaCore.NATURE_ID);
    }

    private static void addKotlinNature(IProject project) throws CoreException {
        addNature(project, KotlinNature.KOTLIN_NATURE);
    }

    private static void addNature(IProject project, String nature) throws CoreException {
        final IProjectDescription description = project.getDescription();
        final String[] prevNatures = description.getNatureIds();
        final String[] newNatures = new String[prevNatures == null ? 1 : prevNatures.length + 1];
        if (prevNatures != null) {
            System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
            newNatures[newNatures.length - 1] = nature;
        }
        description.setNatureIds(newNatures);
        project.setDescription(description, null);
    }
}