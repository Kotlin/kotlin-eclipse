package org.jetbrains.kotlin.ui.editors.navigation

import org.eclipse.core.resources.IStorage
import org.eclipse.core.runtime.IPath
import java.io.InputStream

class StringStorage(private val content: String, private val name: String, private val packageFqName: String) : IStorage {
    override fun getName(): String = name

    override fun getContents(): InputStream? = content.byteInputStream()

    override fun getFullPath(): IPath? = null

    override fun <T> getAdapter(adapter: Class<T>?): T? = null

    override fun isReadOnly(): Boolean = true

    val fqName: String
        get() = "$packageFqName/$name"

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + name.hashCode()
        result = prime * result + packageFqName.hashCode()
        result = prime * result + content.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other !is StringStorage) return false

        return name == other.name &&
                packageFqName == other.packageFqName &&
                content == other.content
    }
}