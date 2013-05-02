package org.jetbrains.kotlin.model;
 
import java.net.URI;
import java.util.Arrays;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.PreferenceConstants;
 
public class KotlinProjectSupport {
    /**
     * For new Kotlin project creates following:
     * - create the default Eclipse project
     * - add the Kotlin project nature
     * - create the simple folder structure
     *
     * @param projectName name of created project
     * @param location location for project
     * @return created project instance
     */
    public static IProject createProject(String projectName, URI location) {
        Assert.isNotNull(projectName);
        Assert.isTrue(!projectName.trim().isEmpty());
 
        IProject project = createBaseProject(projectName, location);
        try {
            addNature(project);
 
            String[] paths = { "bin", "lib", "src" };
            addToProjectStructure(project, paths);
            
            IFolder sourcefolder = project.getFolder("src");
            IClasspathEntry newSourceEntry = JavaCore.newSourceEntry(sourcefolder.getFullPath());
            IClasspathEntry[] defaultJRELibrary = PreferenceConstants.getDefaultJRELibrary();
            IJavaProject javaProject = JavaCore.create(project);
            IClasspathEntry[] entries = Arrays.copyOf(defaultJRELibrary, defaultJRELibrary.length + 1);
            entries[entries.length - 1] = newSourceEntry;
            javaProject.setRawClasspath(entries, null);

        } catch (CoreException e) {
            e.printStackTrace();
            project = null;
        }
 
        return project;
    }
 
    /**
     * Create a basic project.
     *
     * @param location
     * @param projectName
     */
    private static IProject createBaseProject(String projectName, URI location) {
        // it is acceptable to use the ResourcesPlugin class
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IProject newProject = root.getProject(projectName);
 
        if (!newProject.exists()) {
            URI projectLocation = location;
            IProjectDescription desc = newProject.getWorkspace().newProjectDescription(newProject.getName());
            if (location != null && root.getLocationURI().equals(location)) {
                projectLocation = null;
            }
 
            desc.setLocationURI(projectLocation);
            try {
                newProject.create(desc, null);
                if (!newProject.isOpen()) {
                    newProject.open(null);
                }
            } catch (CoreException e) {
                e.printStackTrace();
            }
        }
 
        return newProject;
    }
 
    private static void createFolder(IFolder folder) throws CoreException {
        IContainer parent = folder.getParent();
        if (parent instanceof IFolder) {
            createFolder((IFolder) parent);
        }
        if (!folder.exists()) {
            folder.create(false, true, null);
        }
    }
 
    /**
     * Create a folder structure with a parent root, overlay, and a few child
     * folders.
     *
     * @param newProject
     * @param paths
     * @throws CoreException
     */
    private static void addToProjectStructure(IProject newProject, String[] paths) throws CoreException {
        for (String path : paths) {
            IFolder etcFolders = newProject.getFolder(path);
            createFolder(etcFolders);
        }
    }
 
    private static void addNature(IProject project) throws CoreException {
        if (!project.hasNature(KotlinNature.KOTLIN_NATURE)) {
            IProjectDescription description = project.getDescription();
            String[] prevNatures = description.getNatureIds();
            String[] newNatures = new String[prevNatures.length + 2];
            System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
            newNatures[prevNatures.length] = KotlinNature.KOTLIN_NATURE;
            newNatures[prevNatures.length + 1] = JavaCore.NATURE_ID;
            description.setNatureIds(newNatures);
 
            IProgressMonitor monitor = null;
            project.setDescription(description, monitor);
        }
    }
 
}