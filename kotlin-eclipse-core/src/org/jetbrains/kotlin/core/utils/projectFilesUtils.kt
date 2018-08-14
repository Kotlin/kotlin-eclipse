package org.jetbrains.kotlin.core.utils

import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.IResource
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.core.runtime.Path
import org.eclipse.jdt.core.IClasspathEntry
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.internal.core.JavaProject
import java.io.File

val IFile.isInClasspath: Boolean
    get() {
        val project = JavaCore.create(project) as JavaProject
        val packageRoots = project.getResolvedClasspath(true)
                .asSequence()
                .filter { it.entryKind == IClasspathEntry.CPE_SOURCE }
                .flatMap { project.computePackageFragmentRoots(it).asSequence() }
                .map { it.resource }
                .toSet()

        return generateSequence<IResource>(this) { it.parent }
                .any { it in packageRoots }
    }

val File.asResource: IFile?
    get() = ResourcesPlugin.getWorkspace().root
            .getFileForLocation(Path.fromOSString(absolutePath))

val IFile.asFile: File
	get() = File(locationURI)

val IFile.javaProject: IJavaProject?
    get() = project?.let { JavaCore.create(it) }
