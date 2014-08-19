package org.jetbrains.kotlin.core.model;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.internal.resources.Resource;
import org.eclipse.core.internal.resources.ResourceInfo;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.ExternalFoldersManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.core.filesystem.KotlinFileSystem;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.utils.ProjectUtils;

public class KotlinJavaManager {
    public static final KotlinJavaManager INSTANCE = new KotlinJavaManager();
    
    public static final Path KOTLIN_BIN_FOLDER = new Path("kotlin_bin");
    
    private KotlinJavaManager() {
    }
    
    public void registerKtExternalBinFolder(@NotNull IJavaProject javaProject) {
        if (!hasKotlinBinFolder(javaProject)) {
            addFolderForKotlinClassFiles(javaProject.getProject());
        }
        updateKotlinBinFolderFileSystem(javaProject);
    }
    
    @NotNull
    public IPath getKotlinBinFullPath(@NotNull IProject project) {
        return project.getLocation().append(KOTLIN_BIN_FOLDER);
    }
    
    @NotNull
    public IFolder getKotlinBinFolderFor(@NotNull IProject project) {
        return project.getFolder(KOTLIN_BIN_FOLDER);
    }
    
    private void addFolderForKotlinClassFiles(@NotNull final IProject project) { 
        try {
            IJavaProject javaProject = JavaCore.create(project);
            if (!hasKotlinBinFolder(javaProject)) {
                IFolder folder = project.getFolder(KOTLIN_BIN_FOLDER);
                if (!folder.exists()) {
                    folder.create(true, true, null);
                }
                
                ProjectUtils.addToClasspath(
                        javaProject,
                        JavaCore.newLibraryEntry(getKotlinBinFullPath(javaProject.getProject()), null, null));
            }
        } catch (JavaModelException e) {
            KotlinLogger.logAndThrow(e);
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
    }
    
    private void updateKotlinBinFolderFileSystem(@NotNull IJavaProject javaProject) {
        try {
            IProject externalProject = ExternalFoldersManager.getExternalFoldersManager().getExternalFoldersProject();
            if (!externalProject.exists()) {
                return;
            }
            
            IPath ktBinPath = getKotlinBinFullPath(javaProject.getProject());
            for (IResource member : externalProject.members()) {
                if (ktBinPath.equals(member.getLocation())) {
                    setKtFileSystemFor(member);
                    return;
                }
            }
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
    }
    
    private boolean hasKotlinBinFolder(@NotNull final IJavaProject javaProject) {
        try {
            if (!javaProject.getProject().getFolder(KOTLIN_BIN_FOLDER).exists()) {
                return false;
            }
            
            IPath kotlinBinPath = getKotlinBinFullPath(javaProject.getProject());
            for (IClasspathEntry cp : javaProject.getRawClasspath()) {
                if (kotlinBinPath.equals(cp.getPath())) {
                    return true;
                }
            }
        } catch (JavaModelException e) {
            KotlinLogger.logAndThrow(e);
        }
        
        return false;
    }
    
    private void setKtFileSystemFor(IResource resource) {
        ResourceInfo resourceInfo = ((Resource) resource).getResourceInfo(true, false);
        Workspace workspace = (Workspace) ResourcesPlugin.getWorkspace();
        workspace.getFileSystemManager().setLocation(
                resource, 
                resourceInfo, 
                setKtURIFor(resource.getLocationURI()));
    }
    
    private URI setKtURIFor(URI locationURI) {
        try {
            return new URI(
                    KotlinFileSystem.SCHEME, 
                    locationURI.getUserInfo(), 
                    locationURI.getHost(), 
                    locationURI.getPort(), 
                    locationURI.getPath(), 
                    locationURI.getQuery(), 
                    locationURI.getFragment());
        } catch (URISyntaxException e) {
            KotlinLogger.logAndThrow(e);
            throw new IllegalStateException(e);
        }
    }
}
