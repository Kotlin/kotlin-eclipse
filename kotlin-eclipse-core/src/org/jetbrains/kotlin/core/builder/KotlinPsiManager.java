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
package org.jetbrains.kotlin.core.builder;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.model.KotlinEnvironment;
import org.jetbrains.kotlin.core.model.KotlinLightVirtualFile;
import org.jetbrains.kotlin.core.model.KotlinNature;
import org.jetbrains.kotlin.core.utils.KotlinFilesCollectorUtilsKt;
import org.jetbrains.kotlin.idea.KotlinFileType;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.psi.KtElement;
import org.jetbrains.kotlin.psi.KtFile;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.util.text.StringUtilRt;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.impl.PsiFileFactoryImpl;
import com.intellij.testFramework.LightVirtualFile;

public class KotlinPsiManager {
    
    public static final KotlinPsiManager INSTANCE = new KotlinPsiManager();
    
    private final Map<IProject, Set<IFile>> projectFiles = new HashMap<>();
    private final Map<IFile, KtFile> cachedKtFiles = new HashMap<>();
    
    private final Object mapOperationLock = new Object();
    
    private KotlinPsiManager() {
    }
    
    public void updateProjectPsiSources(@NotNull IFile file, int flag) {
        switch (flag) {
            case IResourceDelta.ADDED:
                addFile(file);
                break;
                
            case IResourceDelta.REMOVED:
                removeFile(file);
                break;
                
            default:
                throw new IllegalArgumentException();
        }
    }
    
    public void updateProjectPsiSources(@NotNull IProject project, int flag) {
        switch (flag) {
            case IResourceDelta.ADDED:
                addProject(project);
                break;
        
            case IResourceDelta.REMOVED:
                removeProject(project);
                break;
        }
    }
    
    private void addProject(@NotNull IProject project) {
        synchronized (mapOperationLock) {
            if (project.isAccessible() && KotlinNature.hasKotlinNature(project)) {
                KotlinFilesCollectorUtilsKt.addFilesToParse(JavaCore.create(project));
            }
        }
    }
    
    private void removeProject(@NotNull IProject project) {
        synchronized (mapOperationLock) {
            Set<IFile> files = getFilesByProject(project);
            projectFiles.remove(project);
            for (IFile file : files) {
                cachedKtFiles.remove(file);
            }
        }
    }
    
    private void addFile(@NotNull IFile file) {
        synchronized (mapOperationLock) {
            assert KotlinNature.hasKotlinNature(file.getProject()) : "Project (" + file.getProject().getName() + ") does not have Kotlin nature";
            assert !exists(file) : "File(" + file.getName() + ") is already added";
            
            IProject project = file.getProject();
            if (!projectFiles.containsKey(project)) {
                projectFiles.put(project, new HashSet<IFile>());
            }
            
            projectFiles.get(project).add(file);
        }
    }
    
    private void removeFile(@NotNull IFile file) {
        synchronized (mapOperationLock) {
            assert exists(file) : "File(" + file.getName() + ") does not contain in the psiFiles";
            
            IProject project = file.getProject();
            
            cachedKtFiles.remove(file);
            projectFiles.get(project).remove(file);
        }
    }
    
    @NotNull
    public Set<IFile> getFilesByProject(@Nullable IProject project) {
        synchronized (mapOperationLock) {
            if (projectFiles.containsKey(project)) {
                return Collections.unmodifiableSet(projectFiles.get(project));
            }
            
            return Collections.emptySet();
        }
    }
    
    @NotNull
    public KtFile getParsedFile(@NotNull IFile file) {
        synchronized (mapOperationLock) {
            assert exists(file) : "File(" + file.getName() + ") does not contain in the psiFiles";
            
            if (!cachedKtFiles.containsKey(file)) {
                KtFile jetFile = parseFile(file);
                cachedKtFiles.put(file, jetFile);
            }
            
            return cachedKtFiles.get(file);
        }
    }
    
    public boolean exists(@NotNull IFile file) {
        synchronized (mapOperationLock) {
            IProject project = file.getProject();
            if (project == null) return false;
            
            Set<IFile> files = projectFiles.get(project);
            return files != null ? files.contains(file) : false;
        }
    }
    
    @NotNull
    public Set<IFile> getFilesByProject(@NotNull String projectName) {
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
        return getFilesByProject(project);
    }
    
    public boolean isKotlinSourceFile(@NotNull IResource resource) throws JavaModelException {
        return isKotlinSourceFile(resource, JavaCore.create(resource.getProject()));
    }
    
    public boolean isKotlinSourceFile(@NotNull IResource resource, @NotNull IJavaProject javaProject) throws JavaModelException {
        if (!(resource instanceof IFile) || !KotlinFileType.INSTANCE.getDefaultExtension().equals(resource.getFileExtension())) {
            return false;
        }

        if (!javaProject.exists()) {
            return false;
        }
        
        if (!KotlinNature.hasKotlinNature(javaProject.getProject())) {
            return false;
        }
        
        IClasspathEntry[] classpathEntries = javaProject.getRawClasspath();
        String resourceRoot = resource.getFullPath().segment(1);
        for (IClasspathEntry classpathEntry : classpathEntries) {
            if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
                if (resourceRoot.equals(classpathEntry.getPath().segment(1))) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    public static boolean isKotlinFile(@NotNull IFile file) {
        return KotlinFileType.INSTANCE.getDefaultExtension().equals(file.getFileExtension());
    }
    
    @Nullable
    private KtFile parseFile(@NotNull IFile file) {
        synchronized (mapOperationLock) {
            try {
                if (!file.exists()) {
                    return null;
                }
                File ioFile = new File(file.getRawLocation().toOSString());
                return parseText(FileUtil.loadFile(ioFile, null, true), file);
            } catch (IOException e) {
                KotlinLogger.logAndThrow(e);
            }
        }
        
        return null;
    }
    
    @NotNull
    private KtFile getParsedFile(@NotNull IFile file, @NotNull String expectedSourceCode) {
        synchronized (mapOperationLock) {
            updatePsiFile(file, expectedSourceCode);
            return getParsedFile(file);
        }
    }
    
    private void updatePsiFile(@NotNull IFile file, @NotNull String sourceCode) {
        String sourceCodeWithouCR = StringUtilRt.convertLineSeparators(sourceCode);
        synchronized (mapOperationLock) {
            assert exists(file): "File(" + file.getName() + ") does not contain in the psiFiles";
            
            PsiFile currentParsedFile = getParsedFile(file);
            if (!currentParsedFile.getText().equals(sourceCodeWithouCR)) {
                KtFile jetFile = parseText(sourceCodeWithouCR, file);
                cachedKtFiles.put(file, jetFile);
            }
        }
    }
    
    @Nullable
    public KtFile parseText(@NotNull String text, @NotNull IFile file) {
        StringUtil.assertValidSeparators(text);
        
        IJavaProject javaProject = JavaCore.create(file.getProject());
        Project project = KotlinEnvironment.getEnvironment(javaProject).getProject();
        
        LightVirtualFile virtualFile = new KotlinLightVirtualFile(file, text);
        virtualFile.setCharset(CharsetToolkit.UTF8_CHARSET);
        
        PsiFileFactoryImpl psiFileFactory = (PsiFileFactoryImpl) PsiFileFactory.getInstance(project);
        
        return (KtFile) psiFileFactory.trySetupPsiForFile(virtualFile, KotlinLanguage.INSTANCE, true, false);
    }
    
    @Nullable
    public static KtFile getKotlinParsedFile(@NotNull IFile file) {
        return INSTANCE.exists(file) ? INSTANCE.getParsedFile(file) : null;
    }
    
    @Nullable
    public static KtFile getKotlinFileIfExist(@NotNull IFile file, @NotNull String sourceCode) {
        return INSTANCE.exists(file) ? INSTANCE.getParsedFile(file, sourceCode) : null;
    }
    
    @Nullable
    public static IFile getEclipseFile(@NotNull KtFile jetFile) {
        VirtualFile virtualFile = jetFile.getVirtualFile();
        return virtualFile != null ? 
                ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(virtualFile.getPath())) : null;
    }
    
    @Nullable
    public static IJavaProject getJavaProject(@NotNull KtElement jetElement) {
        IFile eclipseFile = getEclipseFile(jetElement.getContainingKtFile());
        return eclipseFile != null ? JavaCore.create(eclipseFile.getProject()) : null;
    }
}