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
package org.jetbrains.kotlin.ui.builder

import org.eclipse.jdt.core.ElementChangedEvent
import org.eclipse.jdt.core.IElementChangedListener
import org.eclipse.jdt.core.IJavaElementDelta
import org.eclipse.jdt.core.IJavaProject
import org.jetbrains.kotlin.core.model.KotlinEnvironment
import org.jetbrains.kotlin.core.model.KotlinNature
import org.eclipse.jdt.core.IAnnotation
import org.eclipse.jdt.core.IMember
import org.eclipse.jdt.core.IType
import org.eclipse.jdt.core.ICompilationUnit
import org.eclipse.jdt.core.IJavaElement
import org.jetbrains.kotlin.core.model.KotlinAnalysisProjectCache
import org.eclipse.jdt.core.IJavaModel
import org.eclipse.jdt.core.ITypeParameter
import org.eclipse.jdt.internal.core.ImportContainer
import org.eclipse.jdt.core.IPackageFragment
import org.eclipse.jdt.core.IPackageDeclaration
import org.jetbrains.kotlin.core.model.KotlinAnalysisFileCache

public class KotlinClassPathListener : IElementChangedListener {
    override public fun elementChanged(event: ElementChangedEvent) {
        updateEnvironmentIfClasspathChanged(event.getDelta())
    }
    
    private fun updateEnvironmentIfClasspathChanged(delta: IJavaElementDelta) {
        delta.getAffectedChildren().forEach { updateEnvironmentIfClasspathChanged(it) }
        
        val element = delta.getElement()
        if (element is IJavaProject && element.exists() && KotlinNature.hasKotlinNature(element.getProject())) {
            val flags = delta.getFlags()
            if ((flags and IJavaElementDelta.F_CLASSPATH_CHANGED) != 0 || 
                (flags and IJavaElementDelta.F_RESOLVED_CLASSPATH_CHANGED) != 0) {
                KotlinEnvironment.updateKotlinEnvironment(element.project)
                KotlinAnalysisFileCache.resetCache()
                KotlinAnalysisProjectCache.resetCache(element.project)
            }
        }
    }
}

public class KotlinJavaDeclarationsListener : IElementChangedListener {
    override fun elementChanged(event: ElementChangedEvent) {
        resetCacheIfJavaElementDeclarationChanged(event.getDelta())
    }
    
    private fun resetCacheIfJavaElementDeclarationChanged(delta: IJavaElementDelta) {
        delta.getAffectedChildren().forEach { resetCacheIfJavaElementDeclarationChanged(it) }
        
        val element = delta.getElement()
        when (element) {
            is IType,
            is IMember,
            is ITypeParameter,
            is ImportContainer,
            is IPackageDeclaration -> {
                val javaProject = element.getJavaProject()
                if (javaProject != null) {
                    KotlinAnalysisProjectCache.resetCache(javaProject.project)
                }
            }
        }
    }
}