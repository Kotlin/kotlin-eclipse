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
package org.jetbrains.kotlin.core.resolve

import org.jetbrains.kotlin.descriptors.PackagePartProvider
import org.jetbrains.kotlin.load.kotlin.ModuleMapping
import org.eclipse.jdt.core.IJavaProject
import org.jetbrains.kotlin.core.model.KotlinEnvironment

public class KotlinPackagePartProvider(javaProject: IJavaProject) : PackagePartProvider {
    val roots = KotlinEnvironment.getEnvironment(javaProject).getRoots().filter { it.findChild("META-INF") != null }
    
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
        }.mapNotNull {
            it.findChild("META-INF")
        }.flatMap {
            it.children.filter { it.name.endsWith(ModuleMapping.MAPPING_FILE_EXT) }.toList()
        }.map {
            ModuleMapping.create(it.contentsToByteArray())
        }

        return mappings.map { it.findPackageParts(packageFqName) }.filterNotNull().flatMap { it.parts }.distinct()
    }
}