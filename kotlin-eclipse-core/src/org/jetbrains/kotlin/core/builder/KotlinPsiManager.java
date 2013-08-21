package org.jetbrains.kotlin.core.builder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.jet.plugin.JetLanguage;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.utils.KotlinEnvironment;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.impl.PsiFileFactoryImpl;
import com.intellij.testFramework.LightVirtualFile;

public class KotlinPsiManager {
    
    public static final KotlinPsiManager INSTANCE = new KotlinPsiManager();
    
    private final Map<IProject, List<IFile>> projectFiles = new HashMap<>();
    private final Map<IFile, PsiFile> psiFiles = new HashMap<>();
    
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
            case IResourceDelta.REMOVED:
                removeProject(project);
                break;
        }
    }
    
    public void removeProject(@NotNull IProject project) {
        synchronized (mapOperationLock) {
            List<IFile> files = getFilesByProject(project);
            projectFiles.remove(project);
            for (IFile file : files) {
                psiFiles.remove(file);
            }
        }
    }
    
    public void addFile(@NotNull IFile file) {
        synchronized (mapOperationLock) {
            assert !psiFiles.containsKey(file) : "File(" + file.getName() + ") is already added";
            
            IProject project = file.getProject();
            
            if (!projectFiles.containsKey(project)) {
                projectFiles.put(project, new ArrayList<IFile>());
            }
            
            projectFiles.get(project).add(file);
            try {
                File IOFile = new File(file.getRawLocation().toOSString());
                psiFiles.put(file, parseText(FileUtil.loadFile(IOFile), file));
            } catch (IOException e) {
                KotlinLogger.logAndThrow(e);
            }
        }
    }
    
    public void removeFile(@NotNull IFile file) {
        synchronized (mapOperationLock) {
            assert psiFiles.containsKey(file) : "File(" + file.getName() + ") does not contain in the psiFiles";
            
            IProject project = file.getProject();
            
            psiFiles.remove(file);
            List<IFile> files = projectFiles.get(project);
            files.remove(file);
        }
    }
    
    public void updatePsiFile(@NotNull IFile file, @NotNull String sourceCode) {
        synchronized (mapOperationLock) {
            assert psiFiles.containsKey(file) : "File(" + file.getName() + ") does not contain in the psiFiles";
            
            PsiFile parsedFile = parseText(sourceCode, file);
            psiFiles.put(file, parsedFile);
        }
    }
    
    @NotNull
    public List<IFile> getFilesByProject(@Nullable IProject project) {
        synchronized (mapOperationLock) {
            if (projectFiles.containsKey(project)) {
                return Collections.unmodifiableList(projectFiles.get(project));
            }
            
            return new ArrayList<IFile>();
        }
    }
    
    @NotNull
    public PsiFile getParsedFile(@NotNull IFile file, @NotNull String expectedSourceCode) {
        synchronized (mapOperationLock) {
            PsiFile currentParsedFile = getParsedFile(file);
            
            String sourceCodeWithouCR = expectedSourceCode.replaceAll("\r", "");
            if (!currentParsedFile.getText().equals(sourceCodeWithouCR)) {
                updatePsiFile(file, expectedSourceCode);
            }
            
            return psiFiles.get(file);
        }
    }

    @NotNull
    public PsiFile getParsedFile(@NotNull IFile file) {
        synchronized (mapOperationLock) {
            return psiFiles.get(file);
        }
    }
    
    @NotNull
    public List<IFile> getFilesByProject(@NotNull String projectName) {
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
        return getFilesByProject(project);
    }
    
    @NotNull
    public Collection<IFile> getAllFiles() {
        synchronized (mapOperationLock) {
            return Collections.unmodifiableCollection(psiFiles.keySet());
        }
    }
    
    public boolean isProjectChangedState(@NotNull IResourceDelta delta) {
        return (delta.getFlags() & IResourceDelta.CONTENT) != 0 ||
                (delta.getKind() == IResourceDelta.REMOVED) ||
                (delta.getKind() == IResourceDelta.ADDED);
    }
    
    public boolean isCompatibleResource(@NotNull IResource resource) throws JavaModelException {
        if (!(resource instanceof IFile) || !JetFileType.INSTANCE.getDefaultExtension().equals(resource.getFileExtension())) {
            return false;
        }

        IJavaProject javaProject = JavaCore.create(resource.getProject());
        
        if (!javaProject.exists()) {
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
    
    @Nullable
    private PsiFile parseText(@NotNull String text, IFile file) {
        IJavaProject javaProject = JavaCore.create(file.getProject());
        Project project = KotlinEnvironment.getEnvironment(javaProject).getProject();
        
        String path = file.getRawLocation().toOSString();
        LightVirtualFile virtualFile = new LightVirtualFile(path, JetLanguage.INSTANCE, text);
        virtualFile.setCharset(CharsetToolkit.UTF8_CHARSET);
        
        return ((PsiFileFactoryImpl) PsiFileFactory.getInstance(project)).trySetupPsiForFile(virtualFile, JetLanguage.INSTANCE, true, false);
    }
}