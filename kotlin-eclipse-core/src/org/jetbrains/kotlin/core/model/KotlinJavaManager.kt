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

import org.eclipse.core.resources.IFolder
import org.eclipse.core.resources.IProject
import org.eclipse.core.runtime.Path
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.IType
import org.eclipse.jdt.core.JavaModelException
import org.jetbrains.kotlin.core.filesystem.KotlinFileSystem
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.JetClass

public object KotlinJavaManager {
	public val KOTLIN_BIN_FOLDER: Path = Path("kotlin_bin")
    
    public fun getKotlinBinFolderFor(project: IProject): IFolder = project.getFolder(KOTLIN_BIN_FOLDER)
    
    public fun findEclipseType(jetClass: JetClass, javaProject: IJavaProject): IType? {
        return jetClass.getFqName().let {
            if (it != null) javaProject.findType(it.asString()) else null
        }
    }
    
    public fun hasLinkedKotlinBinFolder(javaProject: IJavaProject): Boolean {
        val folder = javaProject.getProject().getFolder(KotlinJavaManager.KOTLIN_BIN_FOLDER)
        return folder.isLinked() && KotlinFileSystem.SCHEME == folder.getLocationURI().getScheme()
    }
}