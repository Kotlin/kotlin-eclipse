package org.jetbrains.kotlin.core.filesystem;

import java.io.File;
import java.net.URI;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.IFileTree;
import org.eclipse.core.filesystem.provider.FileSystem;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

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
        return super.fetchFileTree(root, monitor);
    }
}
