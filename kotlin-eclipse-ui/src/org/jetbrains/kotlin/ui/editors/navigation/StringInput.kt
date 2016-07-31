package org.jetbrains.kotlin.ui.editors.navigation;

import org.eclipse.core.resources.IStorage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.KtFile;

public class StringInput implements IStorageEditorInput {
    
    private final StringStorage storage;
    private final KtFile ktFile;
    
    StringInput(StringStorage storage, KtFile ktFile) {
        this.storage = storage;
        this.ktFile = ktFile;
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
    
    @Nullable
    public KtFile getKtFile() {
        return ktFile;
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
