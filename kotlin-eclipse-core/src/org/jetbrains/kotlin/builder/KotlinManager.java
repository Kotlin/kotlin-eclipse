package org.jetbrains.kotlin.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.jetbrains.kotlin.parser.KotlinParser;

import com.intellij.lang.ASTNode;

public class KotlinManager {
    
    private final static Map<IProject, List<IFile>> projectFiles = new HashMap<>();
    private final static Map<IFile, ASTNode> psiFiles = new HashMap<>();
    
    public static final String ext = ".kt";
    public static final String binFolder = "bin";
    
    public static void updateProjectPsiSources(IProject project, IFile file, int flag) {
        switch (flag) {
            case IResourceDelta.ADDED:
                List<IFile> iFiles = projectFiles.get(project);
                if (iFiles == null) {
                    projectFiles.put(project, new ArrayList<IFile>(Arrays.asList(file)));
                }
                psiFiles.put(file, new KotlinParser(file).parse());
                break;
                
            case IResourceDelta.CHANGED:
                psiFiles.put(file, new KotlinParser(file).parse());
                break;
                
            case IResourceDelta.REMOVED:
                psiFiles.remove(file);
                List<IFile> files = projectFiles.get(project);
                files.remove(file);
                break;
        }
    }
    
    public static void updateProjectPsiSources(IResource resource, int flag) {
        updateProjectPsiSources(resource.getProject(), resource.getProject().getFile(resource.getProjectRelativePath()), flag);
    }
    
    public static boolean isCompatibleResource(IResource resource) {
        return resource.getType() == IResource.FILE && resource.getName().endsWith(ext) &&
                !resource.getFullPath().segment(1).contentEquals(binFolder);
    }
}
