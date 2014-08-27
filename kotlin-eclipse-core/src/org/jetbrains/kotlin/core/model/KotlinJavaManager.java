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
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.ClasspathEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.core.filesystem.KotlinFileSystem;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.utils.ProjectUtils;

public class KotlinJavaManager {
    public static final KotlinJavaManager INSTANCE = new KotlinJavaManager();
    
    public static final Path KOTLIN_BIN_FOLDER = new Path("kotlin_bin");
    private static final IClasspathEntry KOTLIN_BIN_CLASSPATH_ENTRY = new ClasspathEntry(IPackageFragmentRoot.K_BINARY,
            IClasspathEntry.CPE_LIBRARY,
            KOTLIN_BIN_FOLDER,
            ClasspathEntry.INCLUDE_ALL,
            ClasspathEntry.EXCLUDE_NONE,
            null,
            null,
            null,
            false,
            ClasspathEntry.NO_ACCESS_RULES,
            false,
            ClasspathEntry.NO_EXTRA_ATTRIBUTES);
    
    private KotlinJavaManager() {
    }
    
    public void registerKtExternalBinFolder(@NotNull IJavaProject javaProject) {
        if (!hasKotlinBinFolder(javaProject)) {
            addFolderForKotlinClassFiles(javaProject);
        }
        
        setKtFileSystemFor(getKotlinBinFolderFor(javaProject.getProject()));
    }
    
    @NotNull
    public IPath getKotlinBinFullPath(@NotNull IProject project) {
        return project.getLocation().append(KOTLIN_BIN_FOLDER);
    }
    
    @NotNull
    public IFolder getKotlinBinFolderFor(@NotNull IProject project) {
        return project.getFolder(KOTLIN_BIN_FOLDER);
    }
    
    private void addFolderForKotlinClassFiles(@NotNull IJavaProject javaProject) { 
        try {
            if (!hasKotlinBinFolder(javaProject)) {
                IFolder folder = javaProject.getProject().getFolder(KOTLIN_BIN_FOLDER);
                if (!folder.exists()) {
                    folder.create(true, true, null);
                }
                
                ProjectUtils.addToClasspath(javaProject, KOTLIN_BIN_CLASSPATH_ENTRY);
            }
        } catch (JavaModelException e) {
            KotlinLogger.logAndThrow(e);
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
    }
    
    private boolean hasKotlinBinFolder(@NotNull final IJavaProject javaProject) {
        try {
            if (!javaProject.getProject().getFolder(KOTLIN_BIN_FOLDER).exists()) {
                return false;
            }
            
            for (IClasspathEntry cp : javaProject.getRawClasspath()) {
                if (KOTLIN_BIN_FOLDER.equals(cp.getPath().removeFirstSegments(1))) {
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
