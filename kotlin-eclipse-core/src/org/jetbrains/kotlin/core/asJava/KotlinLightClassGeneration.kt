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
package org.jetbrains.kotlin.core.asJava

import org.eclipse.core.resources.IFile
import org.eclipse.core.runtime.CoreException
import org.eclipse.jdt.core.IJavaProject
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.codegen.CompilationErrorHandler
import org.jetbrains.kotlin.codegen.KotlinCodegenFacade
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.core.filesystem.KotlinLightClassManager
import org.jetbrains.kotlin.core.model.KotlinEnvironment
import org.jetbrains.kotlin.core.model.KotlinJavaManager
import org.jetbrains.kotlin.psi.KtFile
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.fileClasses.NoResolveFileClassesProvider
import org.jetbrains.kotlin.codegen.binding.PsiCodegenPredictor
import org.eclipse.core.runtime.Path
import org.jetbrains.kotlin.fileClasses.getFileClassInternalName
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtClass
import com.intellij.psi.util.PsiTreeUtil

object KotlinLightClassGeneration {
    fun updateLightClasses(javaProject: IJavaProject, affectedFiles: Set<IFile>) {
        if (!KotlinJavaManager.hasLinkedKotlinBinFolder(javaProject)) return
        
        KotlinLightClassManager.getInstance(javaProject).computeLightClassesSources()
        KotlinLightClassManager.getInstance(javaProject).updateLightClasses(affectedFiles)
    }
    
    fun buildLightClasses(
            analysisResult: AnalysisResult, 
            javaProject: IJavaProject, 
            jetFiles: List<KtFile>,
            requestedClassName: String): GenerationState {
        val state = GenerationState(
                KotlinEnvironment.getEnvironment(javaProject).project,
                LightClassBuilderFactory(),
                analysisResult.moduleDescriptor,
                analysisResult.bindingContext,
                jetFiles,
                generateDeclaredClassFilter = object : GenerationState.GenerateClassFilter() {
                    override fun shouldAnnotateClass(classOrObject: KtClassOrObject): Boolean = true
                    
                    override fun shouldGenerateClass(classOrObject: KtClassOrObject): Boolean {
                        val internalName = PsiCodegenPredictor.getPredefinedJvmInternalName(classOrObject, NoResolveFileClassesProvider)
                        return checkByInternalName(internalName, requestedClassName)
                    }
                    
                    override fun shouldGeneratePackagePart(ktFile: KtFile): Boolean {
                        val internalName = NoResolveFileClassesProvider.getFileClassInternalName(ktFile)
                        return checkByInternalName(internalName, requestedClassName)
                    }
                    
                    override fun shouldGenerateScript(script: KtScript): Boolean = false
                })
        
        KotlinCodegenFacade.compileCorrectFiles(state) { exception, fileUrl -> Unit }
        
        return state
    }
    
    private fun checkByInternalName(internalName: String?, requestedClassFileName: String): Boolean {
        if (internalName == null) return false
        
        val classFileName = Path(internalName).lastSegment()
        val requestedInternalName = requestedClassFileName.dropLast(".class".length)
        
        if (requestedInternalName.startsWith(classFileName)) {
            if (requestedInternalName.length == classFileName.length) return true
            
            if (requestedInternalName[classFileName.length] == '$') return true
        }
        
        return false
    }
}