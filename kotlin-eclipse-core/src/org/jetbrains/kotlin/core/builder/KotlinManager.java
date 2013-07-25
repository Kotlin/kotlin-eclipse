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

public class KotlinManager {
    
    private final static Map<IProject, List<IFile>> projectFiles = new ConcurrentHashMap<>();
    private final static Map<IFile, ASTNode> psiFiles = new ConcurrentHashMap<>();
    
    private final static Object mapOperationLock = new Object();
    
    public static void updateProjectPsiSources(IResource resource, int flag) {
        IProject project = resource.getProject();
        
        IFile file;
        if (resource instanceof IFile) {
            file = (IFile) resource;
        } else {
            throw new IllegalArgumentException();
        }
        
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
    
    public static void addFile(IFile file, IProject project) {
        synchronized (mapOperationLock) {
            assert !psiFiles.containsKey(file) : "File(" + file.getName() + ") is already added"; 
            
            List<IFile> iFiles = projectFiles.get(project);
            if (iFiles == null) {
                projectFiles.put(project, new ArrayList<IFile>());
            }
            projectFiles.get(project).add(file);
            psiFiles.put(file, KotlinParser.parse(file));
        }
    }
    
    public static void removeFile(IFile file, IProject project) {
        synchronized (mapOperationLock) {
            assert psiFiles.containsKey(file) : "File(" + file.getName() + ") does not contain in the psiFiles";
            
            psiFiles.remove(file);
            List<IFile> files = projectFiles.get(project);
            files.remove(file);
        }
    }
    
    public static void updatePsiFile(IFile file, String sourceCode) {
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
    
    public static List<IFile> getFilesByProject(IProject project) {
        synchronized (mapOperationLock) {
            if (projectFiles.containsKey(project)) {
                return Collections.unmodifiableList(projectFiles.get(project));
            }
            
            return new ArrayList<IFile>();
        }
    }
    
    public static ASTNode getParsedFile(IFile file, String expectedSourceCode) {
        synchronized (mapOperationLock) {
            ASTNode currentParsedFile = getParsedFile(file);
            
            expectedSourceCode = expectedSourceCode.replaceAll("\r", "");
            if (!currentParsedFile.getText().equals(expectedSourceCode)) {
                updatePsiFile(file, expectedSourceCode);
            }
            
            return psiFiles.get(file);
        }
    }
    
    public static ASTNode getParsedFile(IFile file) {
        synchronized (mapOperationLock) {
            return psiFiles.get(file);
        }
    }
    
    public static List<IFile> getFilesByProject(String projectName) {
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
        return getFilesByProject(project);
    }
    
    public static Collection<IFile> getAllFiles() {
        synchronized (mapOperationLock) {
            return Collections.unmodifiableCollection(psiFiles.keySet());
        }
    }
    
    public static boolean isProjectChangedState(IResourceDelta delta) {
        return (delta.getFlags() & IResourceDelta.CONTENT) != 0 ||
                (delta.getKind() == IResourceDelta.REMOVED) ||
                (delta.getKind() == IResourceDelta.ADDED);
    }
    
    public static boolean isCompatibleResource(IResource resource) throws JavaModelException {
        if (!JetFileType.INSTANCE.getDefaultExtension().equals(resource.getFileExtension())) {
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
