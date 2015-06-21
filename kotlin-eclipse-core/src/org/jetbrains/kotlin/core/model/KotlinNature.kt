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
import kotlin.platform.platformStatic
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.eclipse.core.resources.IResourceDelta

public class KotlinNature: IProjectNature {
    companion object {
        public val KOTLIN_NATURE: String = "org.jetbrains.kotlin.core.kotlinNature"
        public val KOTLIN_BUILDER: String = "org.jetbrains.kotlin.ui.kotlinBuilder"
        
        @platformStatic
        public fun hasKotlinNature(project: IProject) : Boolean {
            return project.hasNature(KOTLIN_NATURE)
        }
        
        @platformStatic
        public fun addNature(project:IProject) {
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
            
            val kotlinBuilderCommand = description.newCommand()
            kotlinBuilderCommand.setBuilderName(KOTLIN_BUILDER)
            
            val newBuildCommands = description.getBuildSpec().toArrayList()
            newBuildCommands.add(kotlinBuilderCommand)
            
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
    
    private fun hasKotlinBuilder(project: IProject) : Boolean {
        return project.getDescription().getBuildSpec().any { 
            KOTLIN_BUILDER == it.getBuilderName()
        }
    }
}