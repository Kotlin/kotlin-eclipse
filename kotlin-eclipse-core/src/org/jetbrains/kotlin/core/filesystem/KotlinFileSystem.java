package org.jetbrains.kotlin.core.filesystem;

import java.io.File;
import java.net.URI;
import java.util.Collections;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.IFileTree;
import org.eclipse.core.filesystem.provider.FileSystem;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.jetbrains.kotlin.core.asJava.KotlinLightClassGeneration;

public class KotlinFileSystem extends FileSystem {

    public static final String SCHEME = "org.jetbrains.kotlin.core.filesystem";
    
    private static KotlinFileSystem instance;
    
    public KotlinFileSystem() {
        instance = this;
    }
    
    @Override
    public IFileStore getStore(URI uri) {
        return new KotlinFileStore(new File(uri.getSchemeSpecificPart()));
    }
    
    @Override
    public boolean canDelete() {
        return true;
    }

    @Override
    public boolean canWrite() {
        return true;
    }

    public static KotlinFileSystem getInstance() {
        return instance;
    }

    @Override
    public IFileTree fetchFileTree(IFileStore root, IProgressMonitor monitor) throws CoreException {
        if (root instanceof KotlinFileStore) {
            IProject eclipseProject = ((KotlinFileStore) root).getProject();
            if (eclipseProject != null) {
                KotlinLightClassGeneration.INSTANCE.updateLightClasses(eclipseProject, Collections.emptySet());
            }
        }
        return super.fetchFileTree(root, monitor);
    }
}
