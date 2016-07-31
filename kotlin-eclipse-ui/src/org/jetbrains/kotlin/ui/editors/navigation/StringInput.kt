package org.jetbrains.kotlin.ui.editors.navigation;

import org.eclipse.core.resources.IStorage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;

public class StringInput implements IStorageEditorInput {
    
    private final StringStorage storage;
    
    StringInput(StringStorage storage) {
        this.storage = storage;
    }
    
    @Override
    public boolean exists() {
        return true;
    }
    
    @Override
    public ImageDescriptor getImageDescriptor() {
        return null;
    }
    
    @Override
    public String getName() {
        return storage.getName();
    }
    
    @Override
    public IPersistableElement getPersistable() {
        return null;
    }
    
    @Override
    public IStorage getStorage() {
        return storage;
    }
    
    @Override
    public String getToolTipText() {
        return storage.getFqName();
    }
    
    @Override
    public <T> T getAdapter(Class<T> required) {
        return storage.getAdapter(required);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StringInput) {
            StringInput inputObj = (StringInput) obj;
            return storage.equals(inputObj.storage);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return storage.hashCode();
    }
}
