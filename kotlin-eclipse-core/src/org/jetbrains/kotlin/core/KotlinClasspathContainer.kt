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
package org.jetbrains.kotlin.core

import org.eclipse.core.runtime.IPath
import org.eclipse.core.runtime.Path
import org.eclipse.jdt.core.IClasspathContainer
import org.eclipse.jdt.core.IClasspathEntry
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.JavaCore
import org.jetbrains.kotlin.core.model.KotlinJavaManager
import org.jetbrains.kotlin.core.utils.ProjectUtils
import kotlin.platform.platformStatic
import java.util.ArrayList

val runtimeContainerId: IPath = Path("org.jetbrains.kotlin.core.KOTLIN_CONTAINER")

fun newExportedLibraryEntry(path: IPath): IClasspathEntry = JavaCore.newLibraryEntry(path, null, null, true)

public class KotlinClasspathContainer(val javaProject: IJavaProject) : IClasspathContainer {
    companion object {
        public val CONTAINER_ENTRY: IClasspathEntry = JavaCore.newContainerEntry(runtimeContainerId)
        private val LIB_RUNTIME_NAME = "kotlin-runtime"
        private val LIB_REFLECT_NAME = "kotlin-reflect"
		
		@platformStatic
        public fun getPathToLightClassesFolder(javaProject: IJavaProject): IPath {
            return Path(javaProject.getProject().getName()).append(KotlinJavaManager.KOTLIN_BIN_FOLDER).makeAbsolute()
        }
    }
    
    override public fun getClasspathEntries() : Array<IClasspathEntry> {
    	val entries = ArrayList<IClasspathEntry>()
		
		val kotlinBinFolderEntry = newExportedLibraryEntry(getPathToLightClassesFolder(javaProject))
		entries.add(kotlinBinFolderEntry)
		
		if (!ProjectUtils.isMavenProject(javaProject.getProject())) {
			val kotlinRuntimeEntry = newExportedLibraryEntry(Path(ProjectUtils.buildLibPath(LIB_RUNTIME_NAME)))
			val kotlinReflectEntry = newExportedLibraryEntry(Path(ProjectUtils.buildLibPath(LIB_REFLECT_NAME)))
			
			entries.add(kotlinRuntimeEntry)
			entries.add(kotlinReflectEntry)
		}
		
        return entries.toTypedArray()
    }
	
	override public fun getDescription() : String = "Kotlin Runtime Library"
			
	override public fun getKind() : Int = IClasspathContainer.K_APPLICATION
	
	override public fun getPath() : IPath = runtimeContainerId
}