/*******************************************************************************
 * Copyright 2000-2016 JetBrains s.r.o.
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
 *******************************************************************************/
package org.jetbrains.kotlin.core.resolve.sources

import java.util.HashMap
import org.eclipse.jdt.core.IPackageFragmentRoot
import java.util.zip.ZipFile
import org.eclipse.core.runtime.Path
import java.util.ArrayList
import org.eclipse.jdt.core.IPackageFragment
import java.util.zip.ZipEntry
import java.io.BufferedInputStream
import java.io.InputStreamReader
import java.io.BufferedReader
import org.jetbrains.kotlin.core.resolve.KotlinSourceIndex
import kotlin.io.use
import org.eclipse.core.resources.ResourcesPlugin
import org.jetbrains.kotlin.core.utils.ProjectUtils
import org.eclipse.core.runtime.IPath

fun getSourcePath(root: IPackageFragmentRoot): IPath? {
    return ProjectUtils.convertToGlobalPath(root.resolvedClasspathEntry?.sourceAttachmentPath)
}

class LibrarySourcesIndex(private val sourcePath: IPath) {
    
    private val index = hashMapOf<String, ArrayList<SourceFile>>()
    private val PACKAGE_LINE_PREFIX = "package "

    init {
        val sourceArchive = ZipFile(sourcePath.toOSString())

        sourceArchive.entries().toList()
                .map { it.getName() }
                .filter { KotlinSourceIndex.isKotlinSource(it) }
                .forEach {
                    val shortName = Path(it).lastSegment()
                    index.getOrPut(shortName) { ArrayList() }.add(SourceFile(it))
                }
    }

    fun resolve(shortName: String, packageFqName: String): String? {
        val sourcesList = index[shortName] ?: return null
        if (sourcesList.size == 1) {
            return sourcesList.first().path
        }
        
        val sourceArchive = ZipFile(sourcePath.toOSString())
        
        for (it in sourcesList) {
            if (it.effectivePackage == null) {
                it.effectivePackage = getPackageName(sourceArchive, sourceArchive.getEntry(it.path))
            }
            if (packageFqName == it.effectivePackage) {
                return it.path
            }
        }
        
        return null
    }

    private fun getPackageName(zipFile: ZipFile, entry: ZipEntry): String? {
        val istream = zipFile.getInputStream(entry)
        val reader = BufferedReader(InputStreamReader(istream))
        return reader.use {
            it.lineSequence()
                    .firstOrNull { it.startsWith(PACKAGE_LINE_PREFIX) }
                    ?.removePrefix(PACKAGE_LINE_PREFIX)
                    ?.trim()
        }
    }
}

private class SourceFile(val path: String, var effectivePackage: String? = null)