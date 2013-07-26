package org.jetbrains.kotlin.model;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.jetbrains.annotations.NotNull;

public class KotlinNature implements IProjectNature {
    private IProject project;
    public static final String KOTLIN_NATURE = "org.jetbrains.kotlin.core.kotlinNature";
    public static final String KOTLIN_BUILDER = "org.jetbrains.kotlin.ui.kotlinBuilder";
    
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

    public static boolean hasKotlinNature(@NotNull final IProject project) throws CoreException {
        return project.hasNature(KOTLIN_NATURE);
    }
    
    public static boolean hasKotlinBuilder(@NotNull final IProject project) throws CoreException {
        ICommand[] buildCommands = project.getDescription().getBuildSpec();
        for (ICommand buildCommand : buildCommands) {
            if (KOTLIN_BUILDER.equals(buildCommand.getBuilderName())) {
                return true;
            }
        }
        
        
        return false;
    }
    
    public static void addBuilder(@NotNull final IProject project) throws CoreException {
        if (!hasKotlinBuilder(project)) {
            IProjectDescription description = project.getDescription();
            ICommand[] oldBuildCommands = description.getBuildSpec();
            
            int oldCountBuildCommands = 0;
            if (oldBuildCommands != null) {
                oldCountBuildCommands = oldBuildCommands.length;
            }
            ICommand[] newBuildCommands = new ICommand[oldCountBuildCommands + 1];
            System.arraycopy(oldBuildCommands, 0, newBuildCommands, 0, oldCountBuildCommands);
            
            newBuildCommands[newBuildCommands.length - 1] = description.newCommand();
            newBuildCommands[newBuildCommands.length - 1].setBuilderName(KOTLIN_BUILDER);
            
            description.setBuildSpec(newBuildCommands);
            project.setDescription(description, null);
        }
    }
    
    public static void addNature(@NotNull final IProject project) throws CoreException {
        if (!hasKotlinNature(project)) {   
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