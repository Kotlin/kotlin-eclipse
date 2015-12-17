/*******************************************************************************
* Copyright 2000-2015 JetBrains s.r.o.
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
package org.jetbrains.kotlin.eclipse.ui.utils

import org.eclipse.jdt.core.IJavaProject
import org.jetbrains.kotlin.core.model.KotlinNature
import org.eclipse.core.resources.IProject
import java.util.ArrayList
import org.eclipse.core.resources.ICommand
import org.jetbrains.kotlin.core.utils.ProjectUtils
import org.jetbrains.kotlin.core.KotlinClasspathContainer
import org.jetbrains.kotlin.core.model.KotlinJavaManager
import org.eclipse.core.runtime.jobs.Job
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.IProgressMonitor

fun unconfigureKotlinProject(javaProject: IJavaProject) {
    val project = javaProject.getProject()
    
    unconfigureKotlinNature(project)
    unconfigureKotlinRuntime(javaProject)
    removeKotlinBinFolder(project)
}

fun unconfigureKotlinNature(project: IProject) {
    if (KotlinNature.hasKotlinNature(project)) {
        val description = project.getDescription()
        val newNatures = description.getNatureIds().filter { it != KotlinNature.KOTLIN_NATURE }
        
        description.setNatureIds(newNatures.toTypedArray())
        project.setDescription(description, null)
    }
}

fun unconfigureKotlinRuntime(javaProject: IJavaProject) {
    if (ProjectUtils.hasKotlinRuntime(javaProject.getProject())) {
        val newEntries = javaProject.getRawClasspath().filterNot {
            ProjectUtils.equalsEntriesPaths(it, KotlinClasspathContainer.CONTAINER_ENTRY)
        }
        
        javaProject.setRawClasspath(newEntries.toTypedArray(), null)
    }
}

fun removeKotlinBinFolder(project: IProject) {
    val kotlinBinFolder = KotlinJavaManager.getKotlinBinFolderFor(project)
    kotlinBinFolder.delete(true, null)
}

fun canBeDeconfigured(project: IProject): Boolean {
    return when {
        KotlinNature.hasKotlinNature(project) || ProjectUtils.hasKotlinRuntime(project) -> true
        else -> false
    }
}

fun isConfigurationMissing(project: IProject): Boolean {
    return when {
        !KotlinNature.hasKotlinNature(project) || !ProjectUtils.hasKotlinRuntime(project) -> true
        else -> false
    }
}

inline fun runJob(name: String, priority: Int = Job.LONG, crossinline action: (IProgressMonitor) -> IStatus) {
    val job = object : Job(name) {
        override fun run(monitor: IProgressMonitor): IStatus {
            return action(monitor)
        }
    }
    
    job.setPriority(priority)
    job.schedule()
}