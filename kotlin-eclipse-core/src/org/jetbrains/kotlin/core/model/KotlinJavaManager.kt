package org.jetbrains.kotlin.core.model;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.core.filesystem.KotlinFileSystem;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.JetClass;

public class KotlinJavaManager {
    public static final KotlinJavaManager INSTANCE = new KotlinJavaManager();
    
    public static final Path KOTLIN_BIN_FOLDER = new Path("kotlin_bin");
    
    private KotlinJavaManager() {
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
    
    public boolean hasLinkedKotlinBinFolder(@NotNull IJavaProject javaProject) {
        IFolder folder = javaProject.getProject().getFolder(KotlinJavaManager.KOTLIN_BIN_FOLDER);
        if (folder.isLinked()) {
            return KotlinFileSystem.SCHEME.equals(folder.getLocationURI().getScheme());
        }
        
        return false;
    }
}
