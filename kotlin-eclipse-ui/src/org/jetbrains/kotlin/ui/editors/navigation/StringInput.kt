package org.jetbrains.kotlin.ui.editors.navigation

<<<<<<< HEAD
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
=======
import org.eclipse.core.resources.IStorage
import org.eclipse.jface.resource.ImageDescriptor
import org.eclipse.ui.IPersistableElement
import org.eclipse.ui.IStorageEditorInput
import org.jetbrains.kotlin.psi.KtFile

class StringInput internal constructor(private val storage: StringStorage, val ktFile: KtFile?) : IStorageEditorInput {
    override fun exists(): Boolean {
        return true
>>>>>>> e209cd9... J2K StringInput and StringStorage: convert and prettify
    }

    override fun getImageDescriptor(): ImageDescriptor? = null
    
<<<<<<< HEAD
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StringInput) {
            StringInput inputObj = (StringInput) obj;
            return storage.equals(inputObj.storage);
=======
    override fun getName(): String? = storage.name
    
    override fun getPersistable(): IPersistableElement? = null

    override fun getStorage(): IStorage? = storage

    override fun getToolTipText(): String? = storage.fqName

    override fun <T> getAdapter(required: Class<T>?): T? = storage.getAdapter(required)

    override fun equals(other: Any?): Boolean {
        if (other is StringInput) {
            return storage.equals(other.storage)
>>>>>>> e209cd9... J2K StringInput and StringStorage: convert and prettify
        }
        return false
    }

    override fun hashCode(): Int = storage.hashCode()
}