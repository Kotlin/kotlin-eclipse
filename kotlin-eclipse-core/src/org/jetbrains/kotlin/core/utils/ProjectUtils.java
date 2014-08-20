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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.ClasspathEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.osgi.framework.Bundle;

import com.intellij.openapi.util.Condition;
import com.intellij.util.containers.ContainerUtil;

public class ProjectUtils {
    
    private final static String LIB_FOLDER = "lib";
    private final static String LIB_EXTENSION = "jar";
    
    public final static String KT_HOME = getKtHome();
    private final static String KT_RUNTIME_FILENAME = buildLibName("kotlin-runtime");
    private final static String KT_RUNTIME_PATH = buildLibPath("kotlin-runtime");
    
    public static final IClasspathEntry KT_RUNTIME_ENTRY = new ClasspathEntry(IPackageFragmentRoot.K_BINARY,
                IClasspathEntry.CPE_LIBRARY,
                new Path(KT_RUNTIME_FILENAME),
                ClasspathEntry.INCLUDE_ALL,
                ClasspathEntry.EXCLUDE_NONE,
                null,
                null,
                null,
                false,
                ClasspathEntry.NO_ACCESS_RULES,
                false,
                ClasspathEntry.NO_EXTRA_ATTRIBUTES);
    
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
        
        return jetFile.getPackageName();
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
    public static List<File> getSrcDirectories(@NotNull IJavaProject javaProject) throws JavaModelException {
        List<File> srcDirectories = new ArrayList<File>();
        
        IClasspathEntry[] classPathEntries = javaProject.getRawClasspath();
        IWorkspaceRoot root = javaProject.getProject().getWorkspace().getRoot();
        for (IClasspathEntry classPathEntry : classPathEntries) {
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
                    file = new File(rootDirectory.toPortableString() + classpath);
                }
                
                libDirectories.add(file);
            }
        }
        
        return libDirectories;
    }
    
    public static void addToClasspath(@NotNull IJavaProject javaProject, @NotNull IClasspathEntry newEntry) throws JavaModelException {
        IClasspathEntry[] oldEntries = javaProject.getRawClasspath();
        IClasspathEntry[] newEntries = new IClasspathEntry[oldEntries.length + 1];
        
        System.arraycopy(oldEntries, 0, newEntries, 0, oldEntries.length);
        newEntries[oldEntries.length] = newEntry;
        
        javaProject.setRawClasspath(newEntries, null);
    }
    
    public static boolean classpathContainsEntry(@NotNull IJavaProject javaProject, @NotNull final IClasspathEntry newEntry) throws JavaModelException {
        return !ContainerUtil.filter(Arrays.asList(javaProject.getRawClasspath()), new Condition<IClasspathEntry>() {
            @Override
            public boolean value(IClasspathEntry entry) {
                return entry.getPath().toString().endsWith(newEntry.getPath().toString());
            }
            
        }).isEmpty();
    }
    
    private static IFolder getLibFolder(@NotNull IProject project) {
        return project.getFolder(LIB_FOLDER);
    }
    
    private static IFile getKotlinRuntime(@NotNull IProject project) {
        return project.getFile(KT_RUNTIME_FILENAME);
    }
    
    public static boolean hasKotlinRuntime(@NotNull IProject project) throws JavaModelException {
        return getLibFolder(project).exists() && getKotlinRuntime(project).exists() && classpathContainsEntry(JavaCore.create(project), KT_RUNTIME_ENTRY);
    }
    
    public static void addKotlinRuntime(@NotNull IProject project) throws CoreException {
        IFolder libFolder = getLibFolder(project);
        if (!libFolder.exists()) {
            libFolder.create(false, true, null);
        }
        
        IFile kotlinRuntime = getKotlinRuntime(project);
        if (!kotlinRuntime.exists()) {
            try {
                kotlinRuntime.create(new FileInputStream(
                        ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(KT_RUNTIME_PATH)).getFullPath().toOSString()),
                        true, 
                        null);
            } catch (FileNotFoundException e) {
                KotlinLogger.logAndThrow(e);
            }

        }
        
        IJavaProject javaProject = JavaCore.create(project);
        if (!classpathContainsEntry(javaProject, KT_RUNTIME_ENTRY)) {
            addToClasspath(javaProject, KT_RUNTIME_ENTRY);
        }
    }
    
    
    private static String buildLibName(String libName) {
        return LIB_FOLDER + "/" + libName + "." + LIB_EXTENSION;
    }
    
    public static String buildLibPath(String libName) {
        return KT_HOME + buildLibName(libName);
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