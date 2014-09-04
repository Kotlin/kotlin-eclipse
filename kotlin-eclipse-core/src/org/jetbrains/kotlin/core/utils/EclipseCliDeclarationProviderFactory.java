package org.jetbrains.kotlin.core.utils;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.resources.IProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.lazy.declarations.DeclarationProviderFactory;
import org.jetbrains.jet.lang.resolve.lazy.declarations.DeclarationProviderFactoryService;
import org.jetbrains.jet.lang.resolve.lazy.declarations.FileBasedDeclarationProviderFactory;
import org.jetbrains.jet.storage.StorageManager;

import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;

public class EclipseCliDeclarationProviderFactory extends DeclarationProviderFactoryService {
    private final IProject eclipseProject;
    
    public EclipseCliDeclarationProviderFactory(@NotNull IProject eclipseProject) {
        this.eclipseProject = eclipseProject;
    }
    
    @Override
    @NotNull
    public DeclarationProviderFactory create(@NotNull Project project, @NotNull StorageManager storageManager,
            @NotNull Collection<? extends JetFile> syntheticFiles, @NotNull GlobalSearchScope filesScope) {
        
        ArrayList<JetFile> allFiles = Lists.newArrayList();
        for (JetFile jetFile : ProjectUtils.getSourceFiles(eclipseProject)) {
            VirtualFile virtualFile = jetFile.getVirtualFile();
            assert virtualFile != null : "Source files should be physical files";
            if (filesScope.contains(virtualFile)) {
                allFiles.add(jetFile);
            }
        }
        
        allFiles.addAll(syntheticFiles);
        
        return new FileBasedDeclarationProviderFactory(storageManager, allFiles);
    }
    
}
