package org.jetbrains.kotlin.core.builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    
    private final static Map<IProject, List<IFile>> projectFiles = new HashMap<>();
    private final static Map<IFile, ASTNode> psiFiles = new HashMap<>();
    
    public final static String binFolder = "bin";

    public static void updateProjectPsiSources(IProject project, IFile file, int flag) {
        switch (flag) {
            case IResourceDelta.ADDED:
                assert !containsPsiFile(file) : "File(" + file.getName() + ") is already added"; 
                
                List<IFile> iFiles = projectFiles.get(project);
                if (iFiles == null) {
                    projectFiles.put(project, new ArrayList<IFile>());
                }
                projectFiles.get(project).add(file);
                psiFiles.put(file, KotlinParser.parse(file));
                break;
                
            case IResourceDelta.CHANGED:
                assert containsPsiFile(file) : "File(" + file.getName() + ") does not contain in the psiFiles";
                
                psiFiles.put(file, KotlinParser.parse(file));
                break;
                
            case IResourceDelta.REMOVED:
                assert containsPsiFile(file) : "File(" + file.getName() + ") does not contain in the psiFiles";
                
                psiFiles.remove(file);
                List<IFile> files = projectFiles.get(project);
                files.remove(file);
                break;
                
            default:
                throw new IllegalArgumentException();
        }
    }
    
    public static void updateProjectPsiSources(IResource resource, int flag) {
        updateProjectPsiSources(resource.getProject(), (IFile) resource, flag);
    }
    
    public static Collection<ASTNode> getAllPsiFiles() {
        return Collections.unmodifiableCollection(psiFiles.values());
    }
    
    public static ArrayList<ASTNode> getPsiFiles(IProject project) {
        List<IFile> filesByProject = projectFiles.get(project);
        ArrayList<ASTNode> psiFilesByProject = new ArrayList<>();
        if (filesByProject == null) {
            return psiFilesByProject; 
        }
        
        for (IFile file : filesByProject) {
            psiFilesByProject.add(psiFiles.get(file));
        }
        
        return psiFilesByProject;
    }
    
    public static List<IFile> getFilesByProject(IProject project) {
        return Collections.unmodifiableList(projectFiles.get(project));
    }
    
    public static List<IFile> getFilesByProject(String projectName) {
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
        return getFilesByProject(project);
    }
    
    public static Set<IFile> getAllFiles() {
        return Collections.unmodifiableSet(psiFiles.keySet());
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
    
    private static boolean containsPsiFile(IFile file) {
        return psiFiles.containsKey(file);
    }
}
