package org.jetbrains.kotlin.core.model;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.ClasspathEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.core.filesystem.KotlinFileSystem;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.utils.ProjectUtils;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.JetClass;

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
        try {
            if (!hasLinkedKotlinBinFolder(javaProject)) {
                addFolderForKotlinClassFiles(javaProject);
            }
            
            if (!ProjectUtils.isPathInClasspath(javaProject, KOTLIN_BIN_FOLDER)) {
                ProjectUtils.addToClasspath(javaProject, KOTLIN_BIN_CLASSPATH_ENTRY);
            }
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
    }
    
    @NotNull
    public IFolder getKotlinBinFolderFor(@NotNull IProject project) {
        return project.getFolder(KOTLIN_BIN_FOLDER);
    }
    
    @Nullable
    public IType findEclipseType(@NotNull JetClass jetClass, @NotNull IJavaProject javaProject) {
        try {
            FqName fqName = jetClass.getFqName();
            if (fqName != null) {
                return javaProject.findType(fqName.asString());
            }
        } catch (JavaModelException e) {
            KotlinLogger.logAndThrow(e);
        }
        
        return null;
    }
    
    public URI setKotlinFileSystemScheme(@NotNull IFolder folder) {
        URI locationURI = folder.getLocationURI();
        try {
            IPath path = new Path(folder.getProject().getName()).append(folder.getProjectRelativePath()).makeAbsolute();
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
        IFolder folder = javaProject.getProject().getFolder(KOTLIN_BIN_FOLDER);
        folder.createLink(setKotlinFileSystemScheme(folder), IResource.REPLACE | IResource.ALLOW_MISSING_LOCAL, null);
    }
    
    private boolean hasLinkedKotlinBinFolder(@NotNull IJavaProject javaProject) {
        IFolder folder = javaProject.getProject().getFolder(KOTLIN_BIN_FOLDER);
        if (folder.isLinked()) {
            return KotlinFileSystem.SCHEME.equals(folder.getLocationURI().getScheme());
        }
        
        return false;
    }
    
}
