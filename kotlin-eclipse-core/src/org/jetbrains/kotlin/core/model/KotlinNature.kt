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
package org.jetbrains.kotlin.core.model

import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.IProjectNature
import org.eclipse.core.resources.ProjectScope
import kotlin.properties.Delegates
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import java.util.LinkedList
import org.eclipse.core.runtime.jobs.Job
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.IStatus
import org.eclipse.jdt.core.JavaCore
import java.util.Collections
import org.eclipse.core.runtime.Status
import org.eclipse.core.resources.ResourcesPlugin
import org.jetbrains.kotlin.core.preferences.KotlinCodeStyleProperties

class KotlinNature: IProjectNature {
    companion object {
        val KOTLIN_NATURE: String = "org.jetbrains.kotlin.core.kotlinNature"
        @JvmField val KOTLIN_BUILDER: String = "org.jetbrains.kotlin.ui.kotlinBuilder"
        
        @JvmStatic
        fun hasKotlinNature(project: IProject) : Boolean {
            return project.hasNature(KOTLIN_NATURE)
        }
        
        @JvmStatic
        fun hasKotlinBuilder(project: IProject) : Boolean {
            if (!project.isAccessible) return false
            
            return project.description.buildSpec.any {
                KOTLIN_BUILDER == it.builderName
            }
        }
        
        @JvmStatic
        fun addNature(project:IProject) {
            if (!hasKotlinNature(project)) {
                val description = project.description
                description.natureIds += KOTLIN_NATURE
                project.setDescription(description, null)
            }
        }
    }

    var eclipseProject: IProject by Delegates.notNull()

    override fun configure() {
        addKotlinBuilder(eclipseProject)
        setPreferredCodeStyle(eclipseProject)
    }

    override fun deconfigure() {
        removeKotlinBuilder(eclipseProject)
        KotlinPsiManager.removeProjectFromManager(eclipseProject)
        KotlinAnalysisFileCache.resetCache()
        KotlinAnalysisProjectCache.resetCache(eclipseProject)
    }

    override fun setProject(project: IProject) {
        eclipseProject = project
    }

    override fun getProject(): IProject = eclipseProject
    
    private fun addKotlinBuilder(project: IProject) {
        if (!hasKotlinBuilder(project)) {
            val description = project.description
            
            val kotlinBuilderCommand = description.newCommand().apply { builderName = KOTLIN_BUILDER }
            
            val newBuildCommands = description.buildSpec.toCollection(LinkedList())
            newBuildCommands.addFirst(kotlinBuilderCommand)

            description.buildSpec = newBuildCommands.toTypedArray()
            project.setDescription(description, null)
        }
    }

    private fun setPreferredCodeStyle(eclipseProject: IProject) {
        KotlinCodeStyleProperties(ProjectScope(eclipseProject)).apply {
            codeStyleId = "KOTLIN_OFFICIAL"
            globalsOverridden = true
            saveChanges()
        }

    }
    
    private fun removeKotlinBuilder(project: IProject) {
		if (hasKotlinBuilder(project)) {
			val description = project.description
			val newBuildCommands = description.buildSpec.filter { it.builderName != KotlinNature.KOTLIN_BUILDER }

            description.buildSpec = newBuildCommands.toTypedArray()
			project.setDescription(description, null)
		}
	}
}

// Place Kotlin Builder before Java Builder to avoid red code in Java about Kotlin references 
fun setKotlinBuilderBeforeJavaBuilder(project: IProject) {
    val job = object : Job("Swap Kotlin builder with Java Builder") {
        override fun run(monitor: IProgressMonitor?): IStatus? {
            val description = project.description
                
            val builders = description.buildSpec.toCollection(LinkedList())
            val kotlinBuilderIndex = builders.indexOfFirst { it.builderName == KotlinNature.KOTLIN_BUILDER }
            val javaBuilderIndex = builders.indexOfFirst { it.builderName == JavaCore.BUILDER_ID }
            
            if (kotlinBuilderIndex >= 0 && javaBuilderIndex >= 0 && javaBuilderIndex < kotlinBuilderIndex) {
                Collections.swap(builders, kotlinBuilderIndex, javaBuilderIndex)

                description.buildSpec = builders.toTypedArray()
                project.setDescription(description, monitor)
            }
            
            return Status.OK_STATUS
        }
    }

    job.rule = ResourcesPlugin.getWorkspace().root
    job.schedule()
}