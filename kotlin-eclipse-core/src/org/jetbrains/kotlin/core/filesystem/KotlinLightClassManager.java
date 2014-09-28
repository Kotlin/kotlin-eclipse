package org.jetbrains.kotlin.core.filesystem;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;

import com.google.common.collect.Lists;

public class KotlinLightClassManager {
    public static final KotlinLightClassManager INSTANCE = new KotlinLightClassManager();
    
    private final ConcurrentMap<File, List<File>> sourceFiles = new ConcurrentHashMap<>();
    
    private KotlinLightClassManager() {
    }
    
    public void putClass(@NotNull File lightClass, @NotNull List<File> sourceIOFiles) {
        sourceFiles.put(lightClass, sourceIOFiles);
    }
    
    public boolean isLightClass(@NotNull File file) {
        return sourceFiles.get(file) != null;
    }
    
    @NotNull
    public List<JetFile> getSourceFiles(@NotNull File lightClass) {
        List<File> sourceIOFiles = sourceFiles.get(lightClass);
        if (sourceIOFiles != null) {
            List<JetFile> jetSourceFiles = Lists.newArrayList();
            for (File sourceFile : sourceIOFiles) {
                IFile[] eclipseFile = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(sourceFile.toURI());
                assert eclipseFile.length == 1 : "By URI found " + eclipseFile.length + " IFiles";
                jetSourceFiles.add(KotlinPsiManager.INSTANCE.getParsedFile(eclipseFile[0]));
            }
            
            return jetSourceFiles;
        }
        
        return Collections.<JetFile>emptyList();
    }
}
