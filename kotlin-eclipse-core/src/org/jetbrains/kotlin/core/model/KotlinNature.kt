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

import org.eclipse.core.resources.ICommand
import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.IProjectDescription
import org.eclipse.core.resources.IProjectNature
import org.eclipse.core.runtime.CoreException
import org.jetbrains.kotlin.core.log.KotlinLogger
import kotlin.properties.Delegates
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.eclipse.core.resources.IResourceDelta
import java.util.LinkedList
import org.eclipse.core.runtime.jobs.Job
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.IStatus
import org.eclipse.jdt.core.JavaCore
import java.util.Collections
import org.eclipse.core.runtime.Status
import org.eclipse.core.resources.ResourcesPlugin

public class KotlinNature: IProjectNature {
    companion object {
        public val KOTLIN_NATURE: String = "org.jetbrains.kotlin.core.kotlinNature"
        public val KOTLIN_BUILDER: String = "org.jetbrains.kotlin.ui.kotlinBuilder"
        
        @JvmStatic
        fun hasKotlinNature(project: IProject) : Boolean {
            return project.hasNature(KOTLIN_NATURE)
        }
        
        @JvmStatic
        fun hasKotlinBuilder(project: IProject) : Boolean {
            if (!project.isAccessible) return false
            
            return project.getDescription().getBuildSpec().any { 
                KOTLIN_BUILDER == it.getBuilderName()
            }
        }
        
        @JvmStatic
        fun addNature(project:IProject) {
            if (!hasKotlinNature(project)) {
                val description = project.getDescription()
                
                val newNatureIds = description.getNatureIds().toArrayList()
                newNatureIds.add(KotlinNature.KOTLIN_NATURE)
                
                description.setNatureIds(newNatureIds.toTypedArray())
                project.setDescription(description, null)
            }
        }
    }
    
    public var eclipseProject: IProject by Delegates.notNull()
    
    override public fun configure() {
        addKotlinBuilder(eclipseProject)
		KotlinPsiManager.INSTANCE.updateProjectPsiSources(eclipseProject, IResourceDelta.ADDED)
    }
    
    override public fun deconfigure() {
        removeKotlinBuilder(eclipseProject)
		KotlinPsiManager.INSTANCE.updateProjectPsiSources(eclipseProject, IResourceDelta.REMOVED)
    }
    
    override public fun setProject(project: IProject) {
        eclipseProject = project
    }
    
    override public fun getProject(): IProject = eclipseProject
    
    private fun addKotlinBuilder(project: IProject) {
        if (!hasKotlinBuilder(project)) {
            val description = project.getDescription()
            
            val kotlinBuilderCommand = description.newCommand().apply { setBuilderName(KOTLIN_BUILDER) }
            
            val newBuildCommands = description.getBuildSpec().toCollection(LinkedList())
            newBuildCommands.addFirst(kotlinBuilderCommand)
            
            description.setBuildSpec(newBuildCommands.toTypedArray())
            project.setDescription(description, null)
        }
    }
    
    private fun removeKotlinBuilder(project: IProject) {
		if (hasKotlinBuilder(project)) {
			val description = project.getDescription()
			val newBuildCommands = description.getBuildSpec().filter { it.getBuilderName() != KotlinNature.KOTLIN_BUILDER }
			
			description.setBuildSpec(newBuildCommands.toTypedArray())
			project.setDescription(description, null)
		}
	}
}

// Place Kotlin Builder before Java Builder to avoid red code in Java about Kotlin references 
fun setKotlinBuilderBeforeJavaBuilder(project: IProject) {
    val job = object : Job("Swap Kotlin builder with Java Builder") {
        override fun run(monitor: IProgressMonitor?): IStatus? {
            val description = project.getDescription()
                
            val builders = description.getBuildSpec().toCollection(LinkedList())
            val kotlinBuilderIndex = builders.indexOfFirst { it.getBuilderName() == KotlinNature.KOTLIN_BUILDER }
            val javaBuilderIndex = builders.indexOfFirst { it.getBuilderName() == JavaCore.BUILDER_ID }
            
            if (kotlinBuilderIndex >= 0 && javaBuilderIndex >= 0 && javaBuilderIndex < kotlinBuilderIndex) {
                Collections.swap(builders, kotlinBuilderIndex, javaBuilderIndex)
                
                description.setBuildSpec(builders.toTypedArray())
                project.setDescription(description, monitor)
            }
            
            return Status.OK_STATUS
        }
    }
    
    job.setRule(ResourcesPlugin.getWorkspace().getRoot())
    job.schedule()
}