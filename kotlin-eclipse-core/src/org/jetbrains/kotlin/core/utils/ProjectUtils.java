package org.jetbrains.kotlin.core.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.plugin.JetMainDetector;

public class ProjectUtils {
    
    public static IFile getMainClass(Collection<IFile> files) {
        IJavaProject javaProject = getJavaProjectFromCollection(files);
        KotlinEnvironment kotlinEnvironment = new KotlinEnvironment(javaProject);
        
        for (IFile file : files) {
            JetFile jetFile = kotlinEnvironment.getJetFile(file);
            if (jetFile == null) {
                continue;
            }
            
            if (JetMainDetector.hasMain(jetFile.getDeclarations())) {
                return file;
            }
        }
                
        return null;
    }
    
    public static IJavaProject getJavaProjectFromCollection(Collection<IFile> files) {
        IJavaProject javaProject = null;
        for (IFile file : files) {
            javaProject = JavaCore.create(file.getProject());
            break;
        }
        
        return javaProject;
    }
    
    public static boolean hasMain(IFile file) {
        return getMainClass(Arrays.asList(file)) != null;
    }
    
    @Nullable
    public static String getPackageByFile(IFile file) {
        IJavaProject javaProject = JavaCore.create(file.getProject());
        JetFile jetFile = new KotlinEnvironment(javaProject).getJetFile(file);
        
        assert jetFile != null;
        
        return jetFile.getPackageName();
    }
    
    public static FqName createPackageClassName(IFile file) {
        String filePackage = getPackageByFile(file);
        if (filePackage == null) {
            return null;
        }
        return PackageClassUtils.getPackageClassFqName(new FqName(filePackage));
    }
    
    @NotNull
    public static List<File> getSrcDirectories(@NotNull IJavaProject javaProject) throws JavaModelException {
        List<File> srcDirectories = new ArrayList<File>();
        
        IClasspathEntry[] classpathEntries = javaProject.getRawClasspath();
        String rootDirectory = javaProject.getProject().getLocation().removeLastSegments(1).toPortableString();
        for (IClasspathEntry classpathEntry : classpathEntries) {
            if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
                File file = new File(rootDirectory + classpathEntry.getPath().toPortableString());
                srcDirectories.add(file);
            }
        }
        
        return srcDirectories;
    }
    
    @NotNull
    public static List<File> getLibDirectories(@NotNull IJavaProject javaProject) throws JavaModelException {
        List<File> libDirectories = new ArrayList<File>();
        
        IClasspathEntry[] classpathEntries = javaProject.getResolvedClasspath(false);
        IPath rootDirectory = javaProject.getProject().getLocation();
        String projectName = rootDirectory.lastSegment();
        
        for (IClasspathEntry classpathEntry : classpathEntries) {
            if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
                String classpath = classpathEntry.getPath().toPortableString();
                File file = new File(classpath);
                
                if (!file.isAbsolute()) {
                    if (classpathEntry.getPath().segment(0).equals(projectName)) {
                        file = new File(rootDirectory.removeLastSegments(1).toPortableString() + classpath);
                    } else {
                        file = new File(rootDirectory.toPortableString() + classpath);
                    }
                }
                
                libDirectories.add(file);
            }
        }
        
        return libDirectories;
    }
}