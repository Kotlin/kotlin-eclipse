package org.jetbrains.kotlin.core.builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.kotlin.parser.KotlinParser;

import com.intellij.lang.ASTNode;

public class KotlinPsiManager {
    
    public static final KotlinPsiManager INSTANCE = new KotlinPsiManager();
    
    private final Map<IProject, List<IFile>> projectFiles = new ConcurrentHashMap<>();
    private final Map<IFile, ASTNode> psiFiles = new ConcurrentHashMap<>();
    
    private final Object mapOperationLock = new Object();
    
    private KotlinPsiManager() {
    }
    
    public void updateProjectPsiSources(IFile file, int flag) {
        IProject project = file.getProject();
        
        switch (flag) {
            case IResourceDelta.ADDED:
                addFile(file, project);
                break;
                
            case IResourceDelta.CHANGED:
                updatePsiFile(file, null);
                break;
                
            case IResourceDelta.REMOVED:
                removeFile(file, project);
                break;
                
            default:
                throw new IllegalArgumentException();
        }
    }
    
    public void addFile(IFile file, IProject project) {
        synchronized (mapOperationLock) {
            assert !psiFiles.containsKey(file) : "File(" + file.getName() + ") is already added"; 
            
            if (!projectFiles.containsKey(project)) {
                projectFiles.put(project, new ArrayList<IFile>());
            }
            projectFiles.get(project).add(file);
            psiFiles.put(file, KotlinParser.parse(file));
        }
    }
    
    public void removeFile(IFile file, IProject project) {
        synchronized (mapOperationLock) {
            assert psiFiles.containsKey(file) : "File(" + file.getName() + ") does not contain in the psiFiles";
            
            psiFiles.remove(file);
            List<IFile> files = projectFiles.get(project);
            files.remove(file);
        }
    }
    
    public void updatePsiFile(IFile file, String sourceCode) {
        synchronized (mapOperationLock) {
            assert psiFiles.containsKey(file) : "File(" + file.getName() + ") does not contain in the psiFiles";
            
            ASTNode parsedFile;
            if (sourceCode != null) {
                parsedFile = KotlinParser.parseText(sourceCode);
            } else {
                parsedFile = KotlinParser.parse(file);
            }
            
            psiFiles.put(file, parsedFile);
        }
    }
    
    public List<IFile> getFilesByProject(IProject project) {
        synchronized (mapOperationLock) {
            if (projectFiles.containsKey(project)) {
                return Collections.unmodifiableList(projectFiles.get(project));
            }
            
            return new ArrayList<IFile>();
        }
    }
    
    public ASTNode getParsedFile(IFile file, String expectedSourceCode) {
        synchronized (mapOperationLock) {
            ASTNode currentParsedFile = getParsedFile(file);
            
            expectedSourceCode = expectedSourceCode.replaceAll("\r", "");
            if (!currentParsedFile.getText().equals(expectedSourceCode)) {
                updatePsiFile(file, expectedSourceCode);
            }
            
            return psiFiles.get(file);
        }
    }
    
    public ASTNode getParsedFile(IFile file) {
        synchronized (mapOperationLock) {
            return psiFiles.get(file);
        }
    }
    
    public List<IFile> getFilesByProject(String projectName) {
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
        return getFilesByProject(project);
    }
    
    public Collection<IFile> getAllFiles() {
        synchronized (mapOperationLock) {
            return Collections.unmodifiableCollection(psiFiles.keySet());
        }
    }
    
    public boolean isProjectChangedState(IResourceDelta delta) {
        return (delta.getFlags() & IResourceDelta.CONTENT) != 0 ||
                (delta.getKind() == IResourceDelta.REMOVED) ||
                (delta.getKind() == IResourceDelta.ADDED);
    }
    
    public boolean isCompatibleResource(IResource resource) throws JavaModelException {
        if (!(resource instanceof IFile) || !JetFileType.INSTANCE.getDefaultExtension().equals(resource.getFileExtension())) {
            return false;
        }

        IJavaProject javaProject = JavaCore.create(resource.getProject());
        if (javaProject == null) {
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
}
