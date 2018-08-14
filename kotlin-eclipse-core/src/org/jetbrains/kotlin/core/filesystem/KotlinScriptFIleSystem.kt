package org.jetbrains.kotlin.core.filesystem

import org.eclipse.core.filesystem.EFS
import org.eclipse.core.filesystem.provider.FileInfo
import org.eclipse.core.filesystem.provider.FileStore
import org.eclipse.core.filesystem.provider.FileSystem
import org.eclipse.core.runtime.CoreException
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.Status
import org.jetbrains.kotlin.core.Activator
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.core.script.EnvironmentProjectsManager
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class KotlinScriptFileSystem : FileSystem() {
    private val handles: ConcurrentMap<URI, EnvironmentFileStore> = ConcurrentHashMap()

    private val entries: MutableMap<String, FSEntry> = ConcurrentHashMap(mapOf("/environments" to Directory(emptySet())))

    override fun canWrite() = true

    override fun canDelete() = true

    override fun getStore(uri: URI): EnvironmentFileStore = handles.getOrPut(uri) { EnvironmentFileStore(uri) }

    inner class EnvironmentFileStore(private val uri: URI) : FileStore() {
        private val path = uri.path.dropLastWhile { it == '/' }

        private val _parent: EnvironmentFileStore? =
            path.substringBeforeLast('/', "")
                .takeIf { it.isNotEmpty() }
                ?.let { getStore(URI(uri.scheme, it, null)) }

        private val isPlaceholderMetadata =
            path.endsWith("/.project") && _parent != null && !EnvironmentProjectsManager.wasCreated(_parent.name)

        override fun getName() = path.substringAfterLast('/')

        override fun toURI() = uri

        override fun childNames(options: Int, monitor: IProgressMonitor?) =
            (entries[path] as? Directory)?.children.orEmpty().toTypedArray()

        override fun getParent() = _parent

        override fun getChild(name: String?): EnvironmentFileStore = getStore(URI(uri.scheme, "$path/$name", null))

        override fun openInputStream(options: Int, monitor: IProgressMonitor?): InputStream =
            when (val entry = entries[path]) {
                is File -> ByteArrayInputStream(entry.content)
                is Directory -> error(EFS.ERROR_WRONG_TYPE, "$uri is a directory")
                null -> if (isPlaceholderMetadata) {
                    ByteArrayInputStream(("""
                        <projectDescription>
                            <name>${parent?.name}</name>
                            <natures>
                                <nature>${EnvironmentRemnantNature.NATURE_ID}</nature>
                            </natures>
                        </projectDescription>
                        """.trimIndent()).toByteArray())
                } else {
                    error(EFS.ERROR_NOT_EXISTS, "$uri is not existing")
                }
            }

        override fun fetchInfo(options: Int, monitor: IProgressMonitor?) = FileInfo(name).apply {
            val entry = entries[path]
            setExists(entry != null || isPlaceholderMetadata)
            isDirectory = entry is Directory
        }

        override fun openOutputStream(options: Int, monitor: IProgressMonitor?): OutputStream {
            val entry = entries[path]
            if (entry is Directory) error(EFS.ERROR_WRONG_TYPE, "$uri is a directory")

            return object: ByteArrayOutputStream() {
                override fun close() {
                    if (entry == null) {
                        val parentDirectory = parent?.path?.let { entries[it] }
                        if (parentDirectory is Directory) {
                            entries[parent!!.path] = Directory(parentDirectory.children + name)
                        } else {
                            error(EFS.ERROR_NOT_EXISTS, "$uri parent is not existing")
                        }
                    }

                    if (entry is File && options and EFS.APPEND != 0) {
                        entries[path] = File(entry.content + this.toByteArray())
                    } else {
                        entries[path] = File(this.toByteArray())
                    }
                }
            }

        }

        override fun delete(options: Int, monitor: IProgressMonitor?) {
            entries.remove(path)
            childStores(EFS.NONE, null).forEach { it.delete(options, monitor) }
        }

        override fun mkdir(options: Int, monitor: IProgressMonitor?): EnvironmentFileStore = apply {
            val entry = entries[path]
            if (entry is File) error(EFS.ERROR_WRONG_TYPE, "$uri is a file")

            if (entry == null) {
                if (options and EFS.SHALLOW == 0) {
                    parent?.mkdir(options, monitor)
                }

                val parentDirectory = parent?.path?.let { entries[it] }
                if (parentDirectory is Directory) {
                    entries[parent!!.path] = Directory(parentDirectory.children + name)
                } else {
                    error(EFS.ERROR_NOT_EXISTS, "$uri parent is not existing")
                }

                entries[path] = Directory(emptySet())
            }
        }
    }

}

private sealed class FSEntry
private class File(val content: ByteArray) : FSEntry()
private class Directory(val children: Set<String>) : FSEntry()

private fun error(code: Int, message: String): Nothing =
    throw CoreException(Status(IStatus.ERROR, Activator.PLUGIN_ID, code, message, null))
