package org.jetbrains.kotlin.ui.editors.navigation

import org.eclipse.core.resources.IStorage
import org.eclipse.core.runtime.IPath
import java.io.InputStream

data class StringStorage(
        private val content: String,
        private val name: String,
        private val packageFqName: String) : IStorage {
    
    override fun getName(): String = name

    override fun getContents(): InputStream? = content.byteInputStream()

    override fun getFullPath(): IPath? = null

    override fun <T> getAdapter(adapter: Class<T>?): T? = null

    override fun isReadOnly(): Boolean = true

    val fqName: String
        get() = "$packageFqName/$name"
}