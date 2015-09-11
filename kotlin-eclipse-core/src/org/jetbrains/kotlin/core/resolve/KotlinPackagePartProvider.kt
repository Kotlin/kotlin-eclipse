package org.jetbrains.kotlin.core.resolve

import org.jetbrains.kotlin.descriptors.PackagePartProvider
import org.jetbrains.kotlin.load.kotlin.ModuleMapping
import org.eclipse.jdt.core.IJavaProject
import org.jetbrains.kotlin.core.model.KotlinEnvironment

public class KotlinPackagePartProvider(javaProject: IJavaProject) : PackagePartProvider {
    val roots = KotlinEnvironment.getEnvironment(javaProject).getRoots()
    
    override fun findPackageParts(packageFqName: String): List<String> {
        val pathParts = packageFqName.split('.')
        val mappings = roots.filter {
            //filter all roots by package path existing
            pathParts.fold(it) {
                parent, part ->
                if (part.isEmpty()) parent
                else  parent.findChild(part) ?: return@filter false
            }
            true
        }.map {
            it.findChild("META-INF")
        }.filterNotNull().flatMap {
            it.children.filter { it.name.endsWith(ModuleMapping.MAPPING_FILE_EXT) }.toList()
        }.map {
            ModuleMapping.create(it.contentsToByteArray())
        }

        return mappings.map { it.findPackageParts(packageFqName) }.filterNotNull().flatMap { it.parts }.distinct()
    }
}