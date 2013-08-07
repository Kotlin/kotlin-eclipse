package org.jetbrains.kotlin.core.builder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.parser.KotlinParser;

import com.intellij.lang.ASTNode;

public class KotlinPsiManager {
    
    public static final KotlinPsiManager INSTANCE = new KotlinPsiManager();
    
    private final Map<IProject, List<IFile>> projectFiles = new HashMap<>();
    private final Map<IFile, ASTNode> psiFiles = new HashMap<>();
    
    private final Object mapOperationLock = new Object();
    
    private KotlinPsiManager() {
    }
    
    public void updateProjectPsiSources(@NotNull IFile file, int flag) {
        switch (flag) {
            case IResourceDelta.ADDED:
                addFile(file);
                break;
                
            case IResourceDelta.CHANGED:
                updatePsiFile(file, null);
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
            psiFiles.put(file, KotlinParser.parse(file));
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
    
    public void updatePsiFile(@NotNull IFile file, String sourceCode) {
        synchronized (mapOperationLock) {
            assert psiFiles.containsKey(file) : "File(" + file.getName() + ") does not contain in the psiFiles";
            
            ASTNode parsedFile;
            if (sourceCode != null) {
                parsedFile = parseText(sourceCode);
            } else {
                parsedFile = KotlinParser.parse(file);
            }
            
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
    public ASTNode getParsedFile(@NotNull IFile file, @NotNull String expectedSourceCode) {
        synchronized (mapOperationLock) {
            ASTNode currentParsedFile = getParsedFile(file);
            
            String sourceCodeWithouCR = expectedSourceCode.replaceAll("\r", "");
            if (!currentParsedFile.getText().equals(sourceCodeWithouCR)) {
                updatePsiFile(file, expectedSourceCode);
            }
            
            return psiFiles.get(file);
        }
    }

    @NotNull
    public ASTNode getParsedFile(@NotNull IFile file) {
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
    
    @NotNull
    private ASTNode parseText(@NotNull String text) {
        try {
            File tempFile;
            tempFile = File.createTempFile("temp", "." + JetFileType.INSTANCE.getDefaultExtension());
            BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile));
            bw.write(text);
            bw.close();
            
            ASTNode parsedFile = new KotlinParser(tempFile).parse();
            
            return parsedFile;
        } catch (IOException e) {
            KotlinLogger.logError(e);
            throw new IllegalStateException(e);
        }
    }
}
