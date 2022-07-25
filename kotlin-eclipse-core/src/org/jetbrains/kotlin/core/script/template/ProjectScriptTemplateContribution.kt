/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.core.script.template

import org.eclipse.core.resources.IFile
import org.eclipse.core.runtime.Path
import org.eclipse.jdt.core.IClasspathEntry
import org.eclipse.jdt.core.JavaCore
import org.jetbrains.kotlin.core.model.KotlinScriptEnvironment
import org.jetbrains.kotlin.core.script.ScriptTemplateContribution
import org.jetbrains.kotlin.core.utils.*
import java.io.File

class ProjectScriptTemplateContribution : ScriptTemplateContribution() {

    override val priority: Int get() = Int.MAX_VALUE

    override fun loadTemplate() = ProjectScriptTemplate::class

    override fun scriptEnvironment(script: File): Map<String, Any?> {
        val file = script.asResource

        val definitionClasspath = file?.let { KotlinScriptEnvironment.getEnvironment(file) }
            ?.definitionClasspath.orEmpty()

        val projectClasspath = projectClasspathForScript(file)
        val allClasspath = (projectClasspath + definitionClasspath)
            .joinToString(separator = ":") { it.absolutePath }

        return mapOf("eclipseProjectClasspath" to allClasspath)
    }

    private fun projectClasspathForScript(file: IFile?): List<File> {
        if (file == null) return emptyList()
        val javaProject = file.javaProject ?: return emptyList()
        if (!file.isInClasspath) return emptyList()

        val projectClasspath = try {
            ProjectUtils.collectClasspathWithDependenciesForLaunch(javaProject, false)
        } catch (e: DependencyResolverException) {
            e.resolvedFiles
        }
        val outputFolders = ProjectUtils.getAllOutputFolders(javaProject).map { it.location.toFile() }
        return projectClasspath + outputFolders
    }

    override fun createClasspath(environment: KotlinScriptEnvironment): Array<IClasspathEntry> =
        (environment.javaProject.rawClasspath + environment.definitionClasspath.map {
            JavaCore.newLibraryEntry(Path(it.absolutePath), null, null)
        }).distinctBy { it.path }.toTypedArray()
}