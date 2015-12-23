package org.jetbrains.kotlin.ui.editors;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.jetbrains.annotations.NotNull;

public class StringStorage implements IStorage {
    
    private final @NotNull String content;
    private final @NotNull String name;
    private final @NotNull String packageFqName;
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + name.hashCode();
        result = prime * result + packageFqName.hashCode();
        result = prime * result + content.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        StringStorage other = (StringStorage) obj;
        return name.equals(other.name) &&
                packageFqName.equals(other.packageFqName) && 
                content.equals(other.content);
    }
    
    StringStorage(@NotNull String input, @NotNull String name, @NotNull String packageFqName) {
      this.content = input;
      this.name = name;
      this.packageFqName = packageFqName;
    }
   
    @Override
    public InputStream getContents() throws CoreException {
      return new ByteArrayInputStream(content.getBytes());
    }
   
    @Override
    public IPath getFullPath() {
      return null;
    }
   
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        return null;
    }
    
    @Override
    public String getName() {
        return name;
    }
   
    @Override
    public boolean isReadOnly() {
      return true;
    }
    
    public String getFqName() {
        return packageFqName + '/' + name;
    }
}
