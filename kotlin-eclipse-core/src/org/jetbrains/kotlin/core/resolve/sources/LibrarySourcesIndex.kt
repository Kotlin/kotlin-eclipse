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

public class LibrarySourcesIndex(private val root: IPackageFragmentRoot) {
    private val index: HashMap<String, ArrayList<SourceFile>>
    private val PACKAGE_LINE_PREFIX = "package "
    
    init {
        val sourcePath = ProjectUtils.convertToGlobalPath(
            root.getResolvedClasspathEntry()?.getSourceAttachmentPath())
        index = HashMap<String, ArrayList<SourceFile>>()
        if (sourcePath != null) {
            val sourceArchive = ZipFile(sourcePath.toOSString())
            val names = sourceArchive.entries()
                .toList()
                .map { it.getName() }
                .filter { KotlinSourceIndex.isKotlinSource(it) }
            names.forEach {
                val shortName = Path(it).lastSegment()
                val listForFile = index.getOrPut(shortName) {ArrayList()}
                listForFile.add(SourceFile(it))
            }
        }
    }
    
    public fun resolve(shortName: String, packageFragment: IPackageFragment): String? {
        val sourcesList = index.get(shortName) ?: return null
        if (sourcesList.size == 1) {
            return sourcesList.first().path
        }
        val packageName = packageFragment.getElementName()
        val sourcePath = ProjectUtils.convertToGlobalPath(
            root.getResolvedClasspathEntry()?.getSourceAttachmentPath())!!
        val sourceArchive = ZipFile(sourcePath.toOSString())
        for (it in sourcesList) {
            if (it.effectivePackage == null) {
                it.effectivePackage = getPackageName(sourceArchive, sourceArchive.getEntry(it.path))
            }
            if (packageName == it.effectivePackage) {
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