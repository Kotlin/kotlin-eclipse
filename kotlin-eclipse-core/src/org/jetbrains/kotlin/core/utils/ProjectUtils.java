package org.jetbrains.kotlin.core.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.plugin.JetMainDetector;

public class ProjectUtils {
    
    public static IFile getMainClass(Collection<IFile> files) {
        KotlinEnvironment kotlinEnvironment = new KotlinEnvironment();
        
        for (IFile file : files) {
            if (JetMainDetector.hasMain(kotlinEnvironment.getJetFile(file).getDeclarations())) {
                return file;
            }
        }
                
        return null;
    }
    
    public static boolean hasMain(IFile file) {
        return getMainClass(Arrays.asList(file)) != null;
    }
    
    public static String getPackageByFile(IFile file) {
        return new KotlinEnvironment().getJetFile(file).getPackageName();
    }
    
    public static FqName createPackageClassName(IFile file) {
        return PackageClassUtils.getPackageClassFqName(new FqName(getPackageByFile(file)));
    }
    
    public static List<File> getSrcDirectories(IJavaProject javaProject) throws JavaModelException {
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
    
    public static List<File> getLibDirectories(IJavaProject javaProject) throws JavaModelException {
        List<File> libDirectories = new ArrayList<File>();
        
        IClasspathEntry[] classpathEntries = javaProject.getRawClasspath();
        for (IClasspathEntry classpathEntry : classpathEntries) {
            if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
                File file = new File(classpathEntry.getPath().toPortableString());
                libDirectories.add(file);
            }
        }
        
        return libDirectories;
    }
}