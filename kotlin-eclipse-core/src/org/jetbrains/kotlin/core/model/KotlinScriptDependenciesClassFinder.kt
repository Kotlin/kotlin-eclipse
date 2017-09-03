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
package org.jetbrains.kotlin.core.model

import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.NonClasspathClassFinder
import com.intellij.psi.PsiElementFinder
import org.eclipse.core.resources.IFile
import org.jetbrains.kotlin.resolve.jvm.KotlinSafeClassFinder
import org.jetbrains.kotlin.script.KotlinScriptDefinitionProvider
import java.io.File
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import java.net.URLClassLoader
import org.eclipse.osgi.internal.loader.EquinoxClassLoader
import java.net.URL

class KotlinScriptDependenciesClassFinder(
        project: Project,
        val eclipseFile: IFile) : NonClasspathClassFinder(project), KotlinSafeClassFinder {
    
    companion object {
        fun resetScriptExternalDependencies(file: IFile) {
            val environment = getEnvironment(file)
            if (environment !is KotlinScriptEnvironment) return

            Extensions.getArea(environment.project)
                    .getExtensionPoint(PsiElementFinder.EP_NAME)
                    .getExtensions()
                    .filterIsInstance(KotlinScriptDependenciesClassFinder::class.java)
                    .forEach {
                        it.clearCache()
                    }
        }
    }
    
    override fun calcClassRoots(): List<VirtualFile> {
        return emptyList()
    }
    
    private fun fromClassLoader(definition: KotlinScriptDefinition): List<File> {
        val classLoader = definition.template.java.classLoader
        return classpathFromClassloader(classLoader)
    }
    
    private fun classpathFromClassloader(classLoader: ClassLoader): List<File> {
        return when (classLoader) {
            is URLClassLoader -> {
                classLoader.urLs
                        ?.mapNotNull { it.toFile() }
                        ?: emptyList()
            }
            is EquinoxClassLoader -> {
                classLoader.classpathManager.hostClasspathEntries.map { entry ->
                    entry.bundleFile.baseFile
                }
            }
            else -> {
                KotlinLogger.logWarning("Could not get dependencies from $classLoader for script provider")
                emptyList()
            }
        }
    }
    
    private fun URL.toFile() =
        try {
            File(toURI().schemeSpecificPart)
        } catch (e: java.net.URISyntaxException) {
            if (protocol != "file") null
            else File(file)
        }

    private fun File.classpathEntryToVfs(): VirtualFile? {
        if (!exists()) return null
        
        val applicationEnvironment = KotlinScriptEnvironment.getEnvironment(eclipseFile).javaApplicationEnvironment
        return when {
            isFile -> applicationEnvironment.jarFileSystem.findFileByPath(this.canonicalPath + "!/")
            isDirectory -> applicationEnvironment.localFileSystem.findFileByPath(this.canonicalPath)
            else -> null
        }
    }
}