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
import org.eclipse.core.runtime.CoreException
import org.eclipse.core.runtime.FileLocator
import org.eclipse.core.runtime.IPath
import org.eclipse.core.runtime.Platform
import org.eclipse.jdt.core.IClasspathEntry
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.core.JavaModelException
import org.jetbrains.kotlin.core.KotlinClasspathContainer
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.core.model.KotlinNature
import org.jetbrains.kotlin.psi.KtFile
import java.io.File
import java.io.IOException
import java.util.*

object ProjectUtils {

    private const val LIB_FOLDER = "lib"
    private const val LIB_EXTENSION = "jar"

    private const val MAVEN_NATURE_ID = "org.eclipse.m2e.core.maven2Nature"
    private const val GRADLE_NATURE_ID = "org.eclipse.buildship.core.gradleprojectnature"

    @JvmStatic
    val KT_HOME = ktHome

    val accessibleKotlinProjects: List<IProject>
        get() = ResourcesPlugin.getWorkspace().root.projects.filter { project -> isAccessibleKotlinProject(project) }

    private val ktHome: String
        get() = try {
            val compilerBundle = Platform.getBundle("org.jetbrains.kotlin.bundled-compiler")
            FileLocator.toFileURL(compilerBundle.getEntry("/")).file
        } catch (e: IOException) {
            KotlinLogger.logAndThrow(e)
        }

    fun getJavaProjectFromCollection(files: Collection<IFile>): IJavaProject? {
        var javaProject: IJavaProject? = null
        for (file in files) {
            javaProject = JavaCore.create(file.project)
            break
        }

        return javaProject
    }

    fun getPackageByFile(file: IFile): String? {
        val jetFile = KotlinPsiManager.getParsedFile(file)

        return jetFile.packageFqName.asString()
    }

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
    fun getOutputFolder(javaProject: IJavaProject): IFolder? = try {
        ResourcesPlugin.getWorkspace().root.findMember(javaProject.outputLocation) as IFolder
    } catch (e: JavaModelException) {
        KotlinLogger.logAndThrow(e)
    }


    fun getSourceFiles(project: IProject): List<KtFile> {
        val jetFiles = ArrayList<KtFile>()
        for (file in KotlinPsiManager.getFilesByProject(project)) {
            val jetFile = KotlinPsiManager.getParsedFile(file)
            jetFiles.add(jetFile)
        }

        return jetFiles
    }

    fun getSourceFilesWithDependencies(javaProject: IJavaProject): List<KtFile> = try {
        val jetFiles = ArrayList<KtFile>()
        for (project in getDependencyProjects(javaProject)) {
            jetFiles.addAll(getSourceFiles(project))
        }
        jetFiles.addAll(getSourceFiles(javaProject.project))

        jetFiles
    } catch (e: JavaModelException) {
        KotlinLogger.logAndThrow(e)
    }


    @Throws(JavaModelException::class)
    fun getDependencyProjects(javaProject: IJavaProject): List<IProject> {
        val projects = ArrayList<IProject>()
        for (classPathEntry in javaProject.getResolvedClasspath(true)) {
            if (classPathEntry.entryKind == IClasspathEntry.CPE_PROJECT) {
                val path = classPathEntry.path
                val project = ResourcesPlugin.getWorkspace().root.getProject(path.toString())
                if (project.isAccessible) {
                    projects.add(project)
                    getDependencyProjects(JavaCore.create(project))
                }
            }
        }

        return projects
    }

    @Throws(JavaModelException::class)
    fun collectClasspathWithDependenciesForBuild(javaProject: IJavaProject): List<File> {
        return expandClasspath(javaProject, true, false) { true }
    }

    @JvmStatic
    @Throws(JavaModelException::class)
    fun collectClasspathWithDependenciesForLaunch(javaProject: IJavaProject): List<File> {
        return expandClasspath(javaProject, true, true) { entry -> entry.entryKind == IClasspathEntry.CPE_LIBRARY }
    }

    @Throws(JavaModelException::class)
    fun expandClasspath(javaProject: IJavaProject, includeDependencies: Boolean,
                        includeBinFolders: Boolean, entryPredicate: Function1<IClasspathEntry, Boolean>): List<File> {
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

        return ArrayList(orderedFiles)
    }

    fun getFileByEntry(entry: IClasspathEntry, javaProject: IJavaProject): List<File> =
            javaProject.findPackageFragmentRoots(entry)
                    .takeIf { it.isNotEmpty() }
                    ?.map { it.resource?.location?.toFile() ?: it.path.toFile() }
                    ?: entry.path.toFile()
                            .takeIf { it.exists() }
                            ?.let { listOf(it) }
                    ?: emptyList()


    @Throws(JavaModelException::class)
    private fun expandDependentProjectClasspath(projectEntry: IClasspathEntry,
                                                includeBinFolders: Boolean, entryPredicate: Function1<IClasspathEntry, Boolean>): List<File> {
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
            val outputFolder = ProjectUtils.getOutputFolder(javaProject)
            if (outputFolder != null && outputFolder.exists()) {
                orderedFiles.add(outputFolder.location.toFile())
            }
        }


        return ArrayList(orderedFiles)
    }

	@JvmStatic
    @Throws(JavaModelException::class)
    fun getSrcDirectories(javaProject: IJavaProject): List<File> {
        return expandClasspath(javaProject, false, false) { entry -> entry.entryKind == IClasspathEntry.CPE_SOURCE }
    }

    @JvmStatic
    @Throws(JavaModelException::class)
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
                                    KotlinLogger.logError("There is no output folder for sources: ${cpe.path.toOSString()}", null)
                            }
                }
    }

    @Throws(JavaModelException::class)
    fun addToClasspath(javaProject: IJavaProject, newEntry: IClasspathEntry) {
        val oldEntries = javaProject.rawClasspath

        val newEntries = arrayOfNulls<IClasspathEntry>(oldEntries.size + 1)
        System.arraycopy(oldEntries, 0, newEntries, 0, oldEntries.size)
        newEntries[oldEntries.size] = newEntry

        javaProject.setRawClasspath(newEntries, null)
    }

	@JvmStatic
    @Throws(JavaModelException::class)
    fun addContainerEntryToClasspath(javaProject: IJavaProject, newEntry: IClasspathEntry) {
        if (!classpathContainsContainerEntry(javaProject.rawClasspath, newEntry)) {
            addToClasspath(javaProject, newEntry)
        }
    }

    fun equalsEntriesPaths(entry: IClasspathEntry, otherEntry: IClasspathEntry): Boolean {
        return entry.path == otherEntry.path
    }

    private fun classpathContainsContainerEntry(entries: Array<IClasspathEntry>,
                                                entry: IClasspathEntry): Boolean {
        return entries.any { classpathEntry -> equalsEntriesPaths(classpathEntry, entry) }
    }

    @Throws(CoreException::class)
    fun hasKotlinRuntime(project: IProject): Boolean {
        return classpathContainsContainerEntry(JavaCore.create(project).rawClasspath,
                KotlinClasspathContainer.CONTAINER_ENTRY)
    }

	@JvmStatic
    @Throws(CoreException::class)
    fun addKotlinRuntime(project: IProject) {
        addKotlinRuntime(JavaCore.create(project))
    }

	@JvmStatic
    @Throws(CoreException::class)
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

    fun isMavenProject(project: IProject): Boolean = try {
            project.hasNature(MAVEN_NATURE_ID)
        } catch (e: CoreException) {
            KotlinLogger.logAndThrow(e)
        }

    fun isGradleProject(project: IProject): Boolean = try {
            project.hasNature(GRADLE_NATURE_ID)
        } catch (e: CoreException) {
            KotlinLogger.logAndThrow(e)
        }


    @JvmStatic
    fun buildLibPath(libName: String): String = KT_HOME + buildLibName(libName)

    fun isAccessibleKotlinProject(project: IProject): Boolean =
            project.isAccessible && KotlinNature.hasKotlinNature(project)

    private fun buildLibName(libName: String): String = "$LIB_FOLDER/$libName.$LIB_EXTENSION"
}