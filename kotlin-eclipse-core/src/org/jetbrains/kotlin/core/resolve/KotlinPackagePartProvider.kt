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

import org.jetbrains.kotlin.core.model.KotlinCommonEnvironment
import org.jetbrains.kotlin.descriptors.PackagePartProvider
import org.jetbrains.kotlin.load.kotlin.ModuleMapping
import org.jetbrains.kotlin.utils.SmartList
import java.io.EOFException

public class KotlinPackagePartProvider(private val environment: KotlinCommonEnvironment) : PackagePartProvider {
    private val notLoadedRoots by lazy(LazyThreadSafetyMode.NONE) {
            environment.getRoots()
            .map { it.file }
            .filter { it.findChild("META-INF") != null }
            .toMutableList()
    }
    
    private val loadedModules: MutableList<ModuleMapping> = SmartList()

    @Synchronized
    override fun findPackageParts(packageFqName: String): List<String> {
        processNotLoadedRelevantRoots(packageFqName)

        return loadedModules.flatMap { it.findPackageParts(packageFqName)?.parts ?: emptySet<String>() }.distinct()
    }

    private fun processNotLoadedRelevantRoots(packageFqName: String) {
        if (notLoadedRoots.isEmpty()) return

        val pathParts = packageFqName.split('.')

        val relevantRoots = notLoadedRoots.filter {
            //filter all roots by package path existing
            pathParts.fold(it) {
                parent, part ->
                if (part.isEmpty()) parent
                else parent.findChild(part) ?: return@filter false
            }
            true
        }
        notLoadedRoots.removeAll(relevantRoots)

        loadedModules.addAll(relevantRoots.mapNotNull {
            it.findChild("META-INF")
        }.flatMap {
            it.children.filter { it.name.endsWith(ModuleMapping.MAPPING_FILE_EXT) }
        }.map { file ->
            try {
                ModuleMapping.create(file.contentsToByteArray(), file.toString())
            }
            catch (e: EOFException) {
                throw RuntimeException("Error on reading package parts for '$packageFqName' package in '$file', roots: $notLoadedRoots", e)
            }
        })
    }
}