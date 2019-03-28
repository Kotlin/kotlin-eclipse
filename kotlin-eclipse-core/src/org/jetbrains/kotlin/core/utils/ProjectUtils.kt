/*******************************************************************************
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.jetbrains.kotlin.core.utils

import org.eclipse.core.resources.*
import org.eclipse.core.runtime.*
import org.eclipse.jdt.core.*
import org.eclipse.jdt.launching.JavaRuntime
import org.jetbrains.kotlin.core.KotlinClasspathContainer
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.core.model.KotlinNature
import org.jetbrains.kotlin.psi.KtFile
import java.io.File
import java.util.*

object ProjectUtils {

    private const val LIB_FOLDER = "lib"
    private const val LIB_EXTENSION = "jar"

    private const val MAVEN_NATURE_ID = "org.eclipse.m2e.core.maven2Nature"
    private const val GRADLE_NATURE_ID = "org.eclipse.buildship.core.gradleprojectnature"

    val accessibleKotlinProjects: List<IProject>
        get() = ResourcesPlugin.getWorkspace().root.projects.filter { project -> isAccessibleKotlinProject(project) }

    @JvmStatic
    val ktHome: String by lazy {
        val compilerBundle = Platform.getBundle("org.jetbrains.kotlin.bundled-compiler")
        FileLocator.toFileURL(compilerBundle.getEntry("/")).file
    }

    fun getJavaProjectFromCollection(files: Collection<IFile>): IJavaProject? =
        files.firstOrNull()
            ?.let { JavaCore.create(it.project) }

    fun getPackageByFile(file: IFile): String? = KotlinPsiManager.getParsedFile(file).packageFqName.asString()

    @JvmStatic
    @JvmOverloads
    fun cleanFolder(container: IContainer?, predicate: (IResource) -> Boolean = { true }) {
        try {
            if (container == null) {
                return
            }
            if (container.exists()) {
                for (member in container.members()) {
                    if (member is IContainer) {
                        cleanFolder(member, predicate)
                    }
                    if (predicate(member)) {
                        if (member.exists()) {
                            member.delete(true, null)
                        }
                    }
                }
            }
        } catch (e: CoreException) {
            KotlinLogger.logError("Error while cleaning folder", e)
        }

    }

    @JvmStatic
    fun getDefaultOutputFolder(javaProject: IJavaProject): IFolder? =
        ResourcesPlugin.getWorkspace().root.findMember(javaProject.outputLocation) as? IFolder

    @JvmStatic
    fun getAllOutputFolders(javaProject: IJavaProject): List<IFolder> =
            javaProject.getResolvedClasspath(true)
                .asSequence()
                .filter { it.entryKind == IClasspathEntry.CPE_SOURCE }
                .map { it.outputLocation }
                .let { it + javaProject.outputLocation }
                .filterNotNull()
                .distinct()
                .mapNotNull { ResourcesPlugin.getWorkspace().root.findMember(it) as? IFolder }
                .filter { it.exists() }
                .toList()

    fun getSourceFiles(project: IProject): List<KtFile> =
        KotlinPsiManager.getFilesByProject(project)
            .map { KotlinPsiManager.getParsedFile(it) }

    fun getSourceFilesWithDependencies(javaProject: IJavaProject): List<KtFile> =
        (getDependencyProjects(javaProject) + javaProject.project)
            .flatMap { getSourceFiles(it) }

    fun getDependencyProjects(javaProject: IJavaProject): List<IProject> =
            javaProject.getResolvedClasspath(true)
                .filter { it.entryKind == IClasspathEntry.CPE_PROJECT }
                .map { ResourcesPlugin.getWorkspace().root.getProject(it.path.toPortableString()) }
                .filter { it.isAccessible }
                .flatMap { listOf(it) + getDependencyProjects(JavaCore.create(it)) }

    fun collectClasspathWithDependenciesForBuild(javaProject: IJavaProject): List<File> {
        return expandClasspath(javaProject, true, false) { true }
    }

    @JvmStatic
    fun collectClasspathWithDependenciesForLaunch(javaProject: IJavaProject, includeJRE: Boolean): List<File> {
        val jreEntries = getJREClasspathElements(javaProject)
        return expandClasspath(javaProject, true, true) {
                entry -> entry.entryKind == IClasspathEntry.CPE_LIBRARY && (includeJRE || jreEntries.none { it.path == entry.path })
        }
    }

    private fun getJREClasspathElements(javaProject: IJavaProject): List<IClasspathEntry> =
        JavaRuntime.resolveRuntimeClasspathEntry(JavaRuntime.computeJREEntry(javaProject), javaProject).map { it.classpathEntry }

    private fun expandClasspath(
        javaProject: IJavaProject, includeDependencies: Boolean,
        includeBinFolders: Boolean, entryPredicate: Function1<IClasspathEntry, Boolean>
    ): List<File> {
        val orderedFiles = LinkedHashSet<File>()

        for (classpathEntry in javaProject.getResolvedClasspath(true)) {
            if (classpathEntry.entryKind == IClasspathEntry.CPE_PROJECT && includeDependencies) {
                orderedFiles.addAll(expandDependentProjectClasspath(classpathEntry, includeBinFolders, entryPredicate))
            } else { // Source folder or library
                if (entryPredicate.invoke(classpathEntry)) {
                    orderedFiles.addAll(getFileByEntry(classpathEntry, javaProject))
                }
            }
        }

        return orderedFiles.toList()
    }

    fun getFileByEntry(entry: IClasspathEntry, javaProject: IJavaProject): List<File> =
        javaProject.findPackageFragmentRoots(entry)
            .takeIf { it.isNotEmpty() }
            ?.map { it.resource?.location?.toFile() ?: it.path.toFile() }
            ?: entry.path.toFile()
                .takeIf { it.exists() }
                ?.let { listOf(it) }
            ?: emptyList()


    private fun expandDependentProjectClasspath(
        projectEntry: IClasspathEntry,
        includeBinFolders: Boolean, entryPredicate: Function1<IClasspathEntry, Boolean>
    ): List<File> {
        val projectPath = projectEntry.path
        val dependentProject = ResourcesPlugin.getWorkspace().root.getProject(projectPath.toString())
        val javaProject = JavaCore.create(dependentProject)

        val orderedFiles = LinkedHashSet<File>()

        for (classpathEntry in javaProject.getResolvedClasspath(true)) {
            if (!(classpathEntry.isExported || classpathEntry.entryKind == IClasspathEntry.CPE_SOURCE)) {
                continue
            }

            if (classpathEntry.entryKind == IClasspathEntry.CPE_PROJECT) {
                orderedFiles.addAll(expandDependentProjectClasspath(classpathEntry, includeBinFolders, entryPredicate))
            } else {
                if (entryPredicate.invoke(classpathEntry)) {
                    orderedFiles.addAll(getFileByEntry(classpathEntry, javaProject))
                }
            }
        }

        if (includeBinFolders) {
            getAllOutputFolders(javaProject)
                .map { it.location.toFile() }
                .toCollection(orderedFiles)
        }

        return orderedFiles.toList()
    }

    @JvmStatic
    fun getSrcDirectories(javaProject: IJavaProject): List<File> =
        expandClasspath(javaProject, false, false) { entry ->
            entry.entryKind == IClasspathEntry.CPE_SOURCE
        }

    @JvmStatic
    fun getSrcOutDirectories(javaProject: IJavaProject): List<Pair<File, File>> {
        val projectOutput = javaProject.outputLocation
        val root = ResourcesPlugin.getWorkspace().root

        return javaProject.getResolvedClasspath(true)
            .filter { it.entryKind == IClasspathEntry.CPE_SOURCE }
            .filter { root.findMember(it.path)?.takeIf(IResource::exists) != null }
            .mapNotNull { cpe ->
                (cpe.outputLocation ?: projectOutput)
                    ?.let { root.findMember(it) }
                    ?.takeIf { it.exists() }
                    ?.let { root.findMember(cpe.path).location.toFile() to it.location.toFile() }
                    .also {
                        if (it == null)
                            KotlinLogger.logError(
                                "There is no output folder for sources: ${cpe.path.toOSString()}",
                                null
                            )
                    }
            }
    }

    fun addToClasspath(javaProject: IJavaProject, vararg newEntries: IClasspathEntry) {
        javaProject.setRawClasspath(javaProject.rawClasspath + newEntries, null)
    }

    @JvmStatic
    fun addContainerEntryToClasspath(javaProject: IJavaProject, newEntry: IClasspathEntry) {
        if (!classpathContainsContainerEntry(javaProject.rawClasspath, newEntry)) {
            addToClasspath(javaProject, newEntry)
        }
    }

    fun equalsEntriesPaths(entry: IClasspathEntry, otherEntry: IClasspathEntry): Boolean {
        return entry.path == otherEntry.path
    }

    private fun classpathContainsContainerEntry(
        entries: Array<IClasspathEntry>,
        entry: IClasspathEntry
    ): Boolean {
        return entries.any { classpathEntry -> equalsEntriesPaths(classpathEntry, entry) }
    }

    fun hasKotlinRuntime(project: IProject): Boolean {
        return classpathContainsContainerEntry(
            JavaCore.create(project).rawClasspath,
            KotlinClasspathContainer.CONTAINER_ENTRY
        )
    }

    @JvmStatic
    fun addKotlinRuntime(project: IProject) {
        addKotlinRuntime(JavaCore.create(project))
    }

    @JvmStatic
    fun addKotlinRuntime(javaProject: IJavaProject) {
        addContainerEntryToClasspath(javaProject, KotlinClasspathContainer.CONTAINER_ENTRY)
    }

    @JvmStatic
    fun convertToGlobalPath(path: IPath?): IPath? {
        if (path == null) {
            return null
        }
        if (path.toFile().exists()) {
            return path.makeAbsolute()
        } else {
            val file = ResourcesPlugin.getWorkspace().root.getFile(path)
            if (file.exists()) {
                return file.rawLocation
            }
        }
        return null

    }

    fun isMavenProject(project: IProject): Boolean = project.hasNature(MAVEN_NATURE_ID)


    fun isGradleProject(project: IProject): Boolean = project.hasNature(GRADLE_NATURE_ID)



    @JvmStatic
    fun buildLibPath(libName: String): String = ktHome + buildLibName(libName)

    fun isAccessibleKotlinProject(project: IProject): Boolean =
        project.isAccessible && KotlinNature.hasKotlinNature(project)

    private fun buildLibName(libName: String): String = "$LIB_FOLDER/$libName.$LIB_EXTENSION"

    fun newExportedLibraryEntry(path: IPath): IClasspathEntry =
        JavaCore.newLibraryEntry(path, null, null, true)

}

fun String.buildLibPath(): Path =
    Path(ProjectUtils.buildLibPath(this))