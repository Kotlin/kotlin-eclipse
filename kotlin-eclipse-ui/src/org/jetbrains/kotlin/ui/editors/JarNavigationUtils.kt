package org.jetbrains.kotlin.ui.editors

import com.intellij.openapi.vfs.VirtualFile
import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.core.runtime.Path
import java.io.File

private val ARCHIVE_EXTENSION = "jar"

fun getAcrhivedFileFromPath(path: String) =
	ResourcesPlugin.getWorkspace().getRoot().getFile(Path(path.replace("!", "")))

fun getFqNameInsideArchive(globalPath: String) =
	globalPath.substringAfterLast(ARCHIVE_EXTENSION).
	substringAfter('!').
	substringAfter(File.separatorChar)

fun isArchived(file :IFile) = 
	file.getFullPath().toOSString().contains(ARCHIVE_EXTENSION)
