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
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.core.KotlinClasspathContainer;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.psi.KtFile;
import org.osgi.framework.Bundle;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import kotlin.collections.ArraysKt;
import kotlin.jvm.functions.Function1;

public class ProjectUtils {
    
    private static final String LIB_FOLDER = "lib";
    private static final String LIB_EXTENSION = "jar";
    
    private static final String MAVEN_NATURE_ID = "org.eclipse.m2e.core.maven2Nature";
    private static final String GRADLE_NATURE_ID = "org.eclipse.buildship.core.gradleprojectnature";
    
    public static final String KT_HOME = getKtHome();
    
    
    public static IFile findFilesWithMain(Collection<IFile> files) {
        for (IFile file : files) {
            KtFile jetFile = KotlinPsiManager.INSTANCE.getParsedFile(file);
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
        KtFile jetFile = KotlinPsiManager.INSTANCE.getParsedFile(file);
        
        assert jetFile != null;
        
        return jetFile.getPackageFqName().asString();
    }
    
    public static void cleanFolder(IContainer container, @NotNull Predicate<IResource> predicate) {
        try {
            if (container == null) {
                return;
            }
            if (container.exists()) {
                for (IResource member : container.members()) {
                    if (member instanceof IContainer) {
                        cleanFolder((IContainer) member, predicate);
                    }
                    if (predicate.apply(member)) {
                        member.delete(true, null);
                    }
                }
            }
        } catch (CoreException e) {
            KotlinLogger.logError("Error while cleaning folder", e);
        }
    }
    
    public static void cleanFolder(IContainer container) throws CoreException {
        cleanFolder(container, Predicates.<IResource>alwaysTrue());
    }
    
    public static IFolder getOutputFolder(@NotNull IJavaProject javaProject) {
        try {
            return (IFolder) ResourcesPlugin.getWorkspace().getRoot().findMember(javaProject.getOutputLocation());
        } catch (JavaModelException e) {
            KotlinLogger.logAndThrow(e);
            throw new IllegalStateException(e);
        }
    }
    
    @NotNull
    public static List<KtFile> getSourceFiles(@NotNull IProject project) {
        List<KtFile> jetFiles = new ArrayList<KtFile>();
        for (IFile file : KotlinPsiManager.INSTANCE.getFilesByProject(project)) {
            KtFile jetFile = KotlinPsiManager.INSTANCE.getParsedFile(file);
            jetFiles.add(jetFile);
        }
        
        return jetFiles;
    }
    
    @NotNull
    public static List<KtFile> getSourceFilesWithDependencies(@NotNull IJavaProject javaProject) {
        try {
            List<KtFile> jetFiles = Lists.newArrayList();
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
    
    public static List<IProject> getDependencyProjects(@NotNull IJavaProject javaProject) throws JavaModelException {
        List<IProject> projects = Lists.newArrayList();
        for (IClasspathEntry classPathEntry : javaProject.getResolvedClasspath(true)) {
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
    
    public static List<File> collectClasspathWithDependenciesForBuild(@NotNull IJavaProject javaProject)
            throws JavaModelException {
        return expandClasspath(javaProject, true, false, Predicates.<IClasspathEntry>alwaysTrue());
    }
    
    public static List<File> collectClasspathWithDependenciesForLaunch(@NotNull IJavaProject javaProject) 
            throws JavaModelException {
        return expandClasspath(javaProject, true, true, new Predicate<IClasspathEntry>() {
            @Override
            public boolean apply(IClasspathEntry entry) {
                return entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY;
            }
        });
    }
    
    @NotNull
    private static List<File> expandClasspath(@NotNull IJavaProject javaProject, boolean includeDependencies,
            boolean includeBinFolders, @NotNull Predicate<IClasspathEntry> entryPredicate) throws JavaModelException {
        Set<File> orderedFiles = Sets.newLinkedHashSet();
        
        for (IClasspathEntry classpathEntry : javaProject.getResolvedClasspath(true)) {
            if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_PROJECT && includeDependencies) {
                orderedFiles.addAll(expandDependentProjectClasspath(classpathEntry, includeBinFolders, entryPredicate));
            } else { // Source folder or library
                if (entryPredicate.apply(classpathEntry)) {
                    orderedFiles.addAll(getFileByEntry(classpathEntry, javaProject));
                }
            }
        }
        
        return Lists.newArrayList(orderedFiles);
    }
    
    @NotNull
    private static List<File> getFileByEntry(@NotNull IClasspathEntry entry, @NotNull IJavaProject javaProject) {
        List<File> files = Lists.newArrayList();
        
        IPackageFragmentRoot[] packageFragmentRoots = javaProject.findPackageFragmentRoots(entry);
        if (packageFragmentRoots.length > 0) {
            for (IPackageFragmentRoot packageFragmentRoot : packageFragmentRoots) {
                IResource resource = packageFragmentRoot.getResource();
                if (resource != null) {
                    files.add(resource.getLocation().toFile());
                } else { // This can be if resource is external
                    files.add(packageFragmentRoot.getPath().toFile());
                }
            }
        } else {
            File file = entry.getPath().toFile();
            if (file.exists()) {
                files.add(file);
            }
        }
        
        return files;
    }
    
    @NotNull
    private static List<File> expandDependentProjectClasspath(@NotNull IClasspathEntry projectEntry,
            boolean includeBinFolders, @NotNull Predicate<IClasspathEntry> entryPredicate) throws JavaModelException {
        IPath projectPath = projectEntry.getPath();
        IProject dependentProject = ResourcesPlugin.getWorkspace().getRoot().getProject(projectPath.toString());
        IJavaProject javaProject = JavaCore.create(dependentProject);
        
        Set<File> orderedFiles = Sets.newLinkedHashSet();
        
        for (IClasspathEntry classpathEntry : javaProject.getResolvedClasspath(true)) {
            if (!(classpathEntry.isExported() || classpathEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE)) {
                continue;
            }
            
            if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
                orderedFiles.addAll(expandDependentProjectClasspath(classpathEntry, includeBinFolders, entryPredicate));
            } else {
                if (entryPredicate.apply(classpathEntry)) {
                    orderedFiles.addAll(getFileByEntry(classpathEntry, javaProject));
                }
            }
        }
        
        if (includeBinFolders) {
            IFolder outputFolder = ProjectUtils.getOutputFolder(javaProject);
            if (outputFolder != null && outputFolder.exists()) {
                orderedFiles.add(outputFolder.getLocation().toFile());
            }
        }
        
        
        return Lists.newArrayList(orderedFiles);
    }
    
    @NotNull
    public static List<File> getSrcDirectories(@NotNull IJavaProject javaProject) throws JavaModelException {
        return expandClasspath(javaProject, false, false, new Predicate<IClasspathEntry>() {
            @Override
            public boolean apply(IClasspathEntry entry) {
                return entry.getEntryKind() == IClasspathEntry.CPE_SOURCE;
            }
        });
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
    
    public static boolean equalsEntriesPaths(@NotNull IClasspathEntry entry, @NotNull IClasspathEntry otherEntry) {
        return entry.getPath().equals(otherEntry.getPath());
    }
    
    private static boolean classpathContainsContainerEntry(@NotNull IClasspathEntry[] entries,
            @NotNull final IClasspathEntry entry) {
        return ArraysKt.<IClasspathEntry>any(entries, new Function1<IClasspathEntry, Boolean>() {
            @Override
            public Boolean invoke(IClasspathEntry classpathEntry) {
                return equalsEntriesPaths(classpathEntry, entry);
            }
        });
    }
    
    public static boolean hasKotlinRuntime(@NotNull IProject project) throws CoreException {
        return classpathContainsContainerEntry(JavaCore.create(project).getRawClasspath(),
                KotlinClasspathContainer.CONTAINER_ENTRY);
    }
    
    public static void addKotlinRuntime(@NotNull IProject project) throws CoreException {
        addKotlinRuntime(JavaCore.create(project));
    }
    
    public static void addKotlinRuntime(@NotNull IJavaProject javaProject) throws CoreException {
        addContainerEntryToClasspath(javaProject, KotlinClasspathContainer.CONTAINER_ENTRY);
    }
    
    @Nullable
    public static IPath convertToGlobalPath(@Nullable IPath path) {
        if (path == null) {
            return null;
        }
        if (path.toFile().exists()) {
            return path.makeAbsolute();
        } else {
            IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
            if (file.exists()) {
                return file.getRawLocation();
            }
        }
        return null;
            
    }
    
    public static boolean isMavenProject(@NotNull IProject project) {
        try {
            return project.hasNature(MAVEN_NATURE_ID);
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
        
        return false;
    }
    
    public static boolean isGradleProject(@NotNull IProject project) {
        try {
            return project.hasNature(GRADLE_NATURE_ID);
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
        
        return false;
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