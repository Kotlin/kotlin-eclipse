package org.jetbrains.kotlin.ui.editors.navigation

import org.eclipse.core.resources.IStorage
import org.eclipse.jface.resource.ImageDescriptor
import org.eclipse.ui.IPersistableElement
import org.eclipse.ui.IStorageEditorInput
import org.jetbrains.kotlin.psi.KtFile

class StringInput internal constructor(private val storage: StringStorage, val ktFile: KtFile?) : IStorageEditorInput {
    override fun exists(): Boolean {
        return true
    }

    override fun getImageDescriptor(): ImageDescriptor? = null
    
    override fun getName(): String? = storage.name
    
    override fun getPersistable(): IPersistableElement? = null

    override fun getStorage(): IStorage? = storage

    override fun getToolTipText(): String? = storage.fqName

    override fun <T> getAdapter(required: Class<T>?): T? = storage.getAdapter(required)

    override fun equals(other: Any?): Boolean {
        if (other is StringInput) {
            return storage.equals(other.storage)
        }
        return false
    }

    override fun hashCode(): Int = storage.hashCode()
}