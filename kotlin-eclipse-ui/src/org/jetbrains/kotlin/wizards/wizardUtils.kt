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
package org.jetbrains.kotlin.wizards

import org.eclipse.jface.viewers.IStructuredSelection
import org.jetbrains.kotlin.core.utils.sourceFolders
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.IPackageFragmentRoot
import org.eclipse.jdt.core.IPackageFragment
import org.eclipse.jdt.core.ICompilationUnit
import org.eclipse.core.resources.IFile
import org.eclipse.jdt.core.JavaCore
import org.eclipse.core.resources.IResource

fun getSourceFolderBySelection(selection: IStructuredSelection): IPackageFragmentRoot? {
    if (selection.isEmpty) return null
    
    val element = selection.firstElement
    return when (element) {
        is IPackageFragmentRoot -> element
        is IJavaProject -> element.sourceFolders.firstOrNull()
        is IPackageFragment -> element.parent as IPackageFragmentRoot
        is ICompilationUnit -> element.parent.parent as IPackageFragmentRoot
        is IResource -> getPackageBySelection(selection)?.parent as? IPackageFragmentRoot
        else -> null
    }
}

fun getPackageBySelection(selection: IStructuredSelection): IPackageFragment? {
    if (selection.isEmpty) return null
    
    val element = selection.firstElement
    return when (element) {
        is IPackageFragment -> element
        is ICompilationUnit -> element.parent as IPackageFragment
        is IResource -> {
            val javaProject = JavaCore.create(element.project)
            javaProject.findPackageFragment(element.getFullPath().removeLastSegments(1))
        }
        else -> null
    }
}