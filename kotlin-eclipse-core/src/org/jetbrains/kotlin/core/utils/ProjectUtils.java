/*******************************************************************************
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *******************************************************************************/
package org.jetbrains.kotlin.core.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.kotlin.core.KotlinClasspathContainer;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.osgi.framework.Bundle;

import com.google.common.collect.Lists;

public class ProjectUtils {
    
    private static final String LIB_FOLDER = "lib";
    private static final String LIB_EXTENSION = "jar";
    
    public static final String KT_HOME = getKtHome();
    
    public static IFile findFilesWithMain(Collection<IFile> files) {
        for (IFile file : files) {
            JetFile jetFile = (JetFile) KotlinPsiManager.INSTANCE.getParsedFile(file);
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
        return findFilesWithMain(Arrays.asList(file)) != null;
    }
    
    @Nullable
    public static String getPackageByFile(IFile file) {
        JetFile jetFile = (JetFile) KotlinPsiManager.INSTANCE.getParsedFile(file);
        
        assert jetFile != null;
        
        return jetFile.getPackageFqName().asString();
    }
    
    public static FqName createPackageClassName(IFile file) {
        String filePackage = getPackageByFile(file);
        if (filePackage == null) {
            return null;
        }
        return PackageClassUtils.getPackageClassFqName(new FqName(filePackage));
    }
    
    public static void cleanFolder(IContainer container) throws CoreException {
        if (container == null) {
            return;
        }
        if (container.exists()) {
            for (IResource member : container.members()) {
                if (member instanceof IContainer) {
                    cleanFolder((IContainer) member);
                }
                member.delete(true, null);
            }
        }
    }
    
    @NotNull
    public static IFolder getOutputFolder(@NotNull IJavaProject javaProject) {
        try {
            return (IFolder) ResourcesPlugin.getWorkspace().getRoot().findMember(javaProject.getOutputLocation());
        } catch (JavaModelException e) {
            KotlinLogger.logAndThrow(e);
            throw new IllegalStateException(e);
        }
    }
    
    @NotNull
    public static List<JetFile> getSourceFiles(@NotNull IProject project) {
        List<JetFile> jetFiles = new ArrayList<JetFile>();
        for (IFile file : KotlinPsiManager.INSTANCE.getFilesByProject(project)) {
            JetFile jetFile = (JetFile) KotlinPsiManager.INSTANCE.getParsedFile(file);
            jetFiles.add(jetFile);
         }
        
        return jetFiles;
    }
    
    @NotNull
    public static List<JetFile> getSourceFilesWithDependencies(@NotNull IJavaProject javaProject) {
        try {
            List<JetFile> jetFiles = Lists.newArrayList();
            for (IProject project : getDependencyProjects(javaProject)) {
                jetFiles.addAll(getSourceFiles(project));
            }
            jetFiles.addAll(getSourceFiles(javaProject.getProject()));
            
            return jetFiles;
        } catch (JavaModelException e) {
            KotlinLogger.logAndThrow(e);
            throw new IllegalStateException(e);
        }
    }
    
    public static boolean isPathOnClasspath(@NotNull IJavaProject javaProject, @NotNull IPath path) {
        try {
            for (IClasspathEntry cp : javaProject.getRawClasspath()) {
                if (path.equals(cp.getPath().removeFirstSegments(1))) {
                    return true;
                }
            }
        } catch (JavaModelException e) {
            KotlinLogger.logAndThrow(e);
        }
        
        return false;
    }
    
    public static List<File> collectDependenciesClasspath(@NotNull IJavaProject javaProject) throws JavaModelException {
        List<File> dependencies = Lists.newArrayList();
        for (IProject project : getDependencyProjects(javaProject)) {
            dependencies.addAll(getSrcDirectories(JavaCore.create(project)));
        }
        
        return dependencies;
    }
    
    public static List<IProject> getDependencyProjects(@NotNull IJavaProject javaProject) throws JavaModelException {
        List<IProject> projects = Lists.newArrayList();
        for (IClasspathEntry classPathEntry : javaProject.getRawClasspath()) {
            if (classPathEntry.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
                IPath path = classPathEntry.getPath();
                IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(path.toString());
                if (project.exists()) {
                    projects.add(project);
                    getDependencyProjects(JavaCore.create(project));
                }
            }
        }
        
        return projects;
    }
    
    @NotNull
    public static List<File> getSrcDirectories(@NotNull IJavaProject javaProject) throws JavaModelException {
        List<File> srcDirectories = new ArrayList<File>();
        
        IWorkspaceRoot root = javaProject.getProject().getWorkspace().getRoot();
        for (IClasspathEntry classPathEntry : javaProject.getRawClasspath()) {
            if (classPathEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
                IPath classPathEntryPath = classPathEntry.getPath();
                IResource classPathResource = root.findMember(classPathEntryPath);
                String path;
                if (classPathResource == null) {
                    path = classPathEntryPath.toOSString();
                } else {
                    path = classPathResource.getLocation().toOSString();
                }
                
                if (!path.isEmpty()) {
                    srcDirectories.add(new File(path));
                }
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
                
                if (classpathEntry.getPath().segment(0).equals(projectName)) {
                    file = new File(rootDirectory.removeLastSegments(1).toPortableString() + classpath);
                } else if (!file.isAbsolute()) {
                    IFile workspaceFile = ResourcesPlugin.getWorkspace().getRoot().getFile(classpathEntry.getPath());
                    if (workspaceFile.exists()) {
                        file = workspaceFile.getLocation().toFile();
                    } else {
                        file = new File(rootDirectory.toPortableString() + classpath);
                    }
                }
                
                libDirectories.add(file);
            }
        }
        
        return libDirectories;
    }
    
    public static void addToClasspath(@NotNull IJavaProject javaProject, @NotNull IClasspathEntry newEntry)
            throws JavaModelException {
        IClasspathEntry[] oldEntries = javaProject.getRawClasspath();
        
        IClasspathEntry[] newEntries = new IClasspathEntry[oldEntries.length + 1];
        System.arraycopy(oldEntries, 0, newEntries, 0, oldEntries.length);
        newEntries[oldEntries.length] = newEntry;
        
        javaProject.setRawClasspath(newEntries, null);
    }
    
    public static void addContainerEntryToClasspath(@NotNull IJavaProject javaProject, @NotNull IClasspathEntry newEntry)
            throws JavaModelException {
        if (!classpathContainsContainerEntry(javaProject.getRawClasspath(), newEntry)) {
            addToClasspath(javaProject, newEntry);
        }
    }
    
    private static boolean classpathContainsContainerEntry(@NotNull IClasspathEntry[] entries,
            @NotNull IClasspathEntry entry) {
        return Arrays.asList(entries).contains(entry);
    }
    
    public static boolean hasKotlinRuntime(@NotNull IProject project) throws CoreException {
        return classpathContainsContainerEntry(JavaCore.create(project).getRawClasspath(),
                KotlinClasspathContainer.getKotlinRuntimeContainerEntry());
    }
    
    public static void addKotlinRuntime(@NotNull IProject project) throws CoreException {
        addKotlinRuntime(JavaCore.create(project));
    }
    
    public static void addKotlinRuntime(@NotNull IJavaProject javaProject) throws CoreException {
        addContainerEntryToClasspath(javaProject, KotlinClasspathContainer.getKotlinRuntimeContainerEntry());
    }
    
    public static String buildLibPath(String libName) {
        return KT_HOME + buildLibName(libName);
    }
    
    private static String buildLibName(String libName) {
        return LIB_FOLDER + "/" + libName + "." + LIB_EXTENSION;
    }
    
    private static String getKtHome() {
        try {
            Bundle compilerBundle = Platform.getBundle("org.jetbrains.kotlin.bundled-compiler");
            return FileLocator.toFileURL(compilerBundle.getEntry("/")).getFile();
        } catch (IOException e) {
            KotlinLogger.logAndThrow(e);
        }
        
        return null;
    }
}