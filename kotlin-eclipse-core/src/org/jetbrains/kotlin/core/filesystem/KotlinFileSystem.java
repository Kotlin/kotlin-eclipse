package org.jetbrains.kotlin.core.filesystem;

import java.io.File;
import java.net.URI;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileSystem;

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
    
    public static KotlinFileSystem getInstance() {
        return instance;
    }
}
