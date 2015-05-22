package org.jetbrains.kotlin.core.filesystem;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.backend.common.output.OutputFile;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.core.asJava.LightClassFile;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.model.KotlinJavaManager;
import org.jetbrains.kotlin.core.utils.ProjectUtils;
import org.jetbrains.kotlin.psi.JetFile;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

public class KotlinLightClassManager {
    public static final KotlinLightClassManager INSTANCE = new KotlinLightClassManager();
    
    private final ConcurrentMap<File, List<File>> sourceFiles = new ConcurrentHashMap<>();
    
    private KotlinLightClassManager() {
    }
    
    @NotNull
    public List<JetFile> getSourceFiles(@NotNull File lightClass) {
        List<File> sourceIOFiles = sourceFiles.get(lightClass);
        if (sourceIOFiles != null) {
            List<JetFile> jetSourceFiles = Lists.newArrayList();
            for (File sourceFile : sourceIOFiles) {
                JetFile jetFile = getJetFileBySourceFile(sourceFile);
                if (jetFile != null) {
                    jetSourceFiles.add(jetFile);
                }
            }
            
            return jetSourceFiles;
        }
        
        return Collections.<JetFile>emptyList();
    }
    
//    Calls after project build
    public void saveKotlinDeclarationClasses(
            @NotNull GenerationState state, 
            @NotNull IJavaProject javaProject,
            @NotNull Set<IFile> affectedFiles) throws CoreException {
        IProject project = javaProject.getProject();
        Map<File, List<File>> newSourceFilesMap = new HashMap<>();
        for (OutputFile outputFile : state.getFactory().asList()) {
            IPath path = KotlinJavaManager.KOTLIN_BIN_FOLDER.append(new Path(outputFile.getRelativePath()));
            LightClassFile lightClassFile = new LightClassFile(project.getFile(path));
            createParentDirsFor(lightClassFile);
            
            lightClassFile.createIfNotExists();
            
            List<File> newSourceFiles = outputFile.getSourceFiles();
            List<File> oldSourceFiles = getIOSourceFiles(lightClassFile.asFile()); // Affected files also contains removed files
            if (containsAffectedFile(newSourceFiles, affectedFiles) || containsAffectedFile(oldSourceFiles, affectedFiles)) {
                lightClassFile.touchFile();
            }
            
            newSourceFilesMap.put(lightClassFile.asFile(), newSourceFiles);
        }
        
        sourceFiles.clear();
        sourceFiles.putAll(newSourceFilesMap);
        
        cleanDeprectedLightClasses(project);
    }

    private void cleanDeprectedLightClasses(IProject project) throws CoreException {
        ProjectUtils.cleanFolder(KotlinJavaManager.INSTANCE.getKotlinBinFolderFor(project), new Predicate<IResource>() {
            @Override
            public boolean apply(IResource resource) {
                if (resource instanceof IFile) {
                    IFile file = (IFile) resource;
                    LightClassFile lightClass = new LightClassFile(file);
                    return KotlinLightClassManager.INSTANCE.getIOSourceFiles(lightClass.asFile()).isEmpty();
                }
                
                return false;
            }
        });
    }
    
    private boolean containsAffectedFile(@NotNull List<File> sourceFiles, @NotNull Set<IFile> affectedFiles) {
        for (File sourceFile : sourceFiles) {
            IFile file = KotlinLightClassManager.getEclipseFile(sourceFile);
            assert file != null : "IFile for source file: " + sourceFile.getName() + " is null";
            
            if (affectedFiles.contains(file)) {
                return true;
            }
        }
        
        return false;
    }
    
    private void createParentDirsFor(@NotNull LightClassFile lightClassFile) {
        IFolder parent = (IFolder) lightClassFile.getResource().getParent();
        if (parent != null && !parent.exists()) {
            createParentDirs(parent);
        }
    }
    
    private void createParentDirs(IFolder folder) {
        IContainer parent = folder.getParent();
        if (!parent.exists()) {
            createParentDirs((IFolder) parent);
        }
        
        try {
            folder.create(true, true, null);
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
    }
    
    @NotNull
    private List<File> getIOSourceFiles(@NotNull File lightClass) {
        List<File> sourceIOFiles = sourceFiles.get(lightClass);
        return sourceIOFiles != null ? sourceIOFiles : Collections.<File>emptyList();
    }
    
    @Nullable
    private static IFile getEclipseFile(@NotNull File sourceFile) {
        IFile[] eclipseFile = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(sourceFile.toURI());
        return eclipseFile.length == 1 ? eclipseFile[0] : null;
    }
    
    @Nullable
    private static JetFile getJetFileBySourceFile(@NotNull File sourceFile) {
        IFile file = getEclipseFile(sourceFile);
        return file != null ? KotlinPsiManager.getKotlinParsedFile(file) : null;
    }
}
