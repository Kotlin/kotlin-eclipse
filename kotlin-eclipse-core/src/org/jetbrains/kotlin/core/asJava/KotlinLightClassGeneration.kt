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
import org.eclipse.core.resources.IProject
import org.eclipse.core.runtime.Path
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.codegen.KotlinCodegenFacade
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.core.filesystem.KotlinLightClassManager
import org.jetbrains.kotlin.core.model.KotlinEnvironment
import org.jetbrains.kotlin.core.model.KotlinJavaManager
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.fileClasses.getFileClassInternalName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.lexer.KtTokens

object KotlinLightClassGeneration {
    fun updateLightClasses(project: IProject, affectedFiles: Set<IFile>) {
        if (!KotlinJavaManager.hasLinkedKotlinBinFolder(project)) return
        
        KotlinLightClassManager.getInstance(project).computeLightClassesSources()
        KotlinLightClassManager.getInstance(project).updateLightClasses(affectedFiles)
    }
    
    fun buildLightClasses(
            analysisResult: AnalysisResult, 
            eclipseProject: IProject, 
            jetFiles: List<KtFile>,
            requestedClassName: String): GenerationState {
        val state = GenerationState.Builder(
                KotlinEnvironment.getEnvironment(eclipseProject).project,
                LightClassBuilderFactory(),
                analysisResult.moduleDescriptor,
                analysisResult.bindingContext,
                jetFiles,
                CompilerConfiguration.EMPTY)
        	.generateDeclaredClassFilter(object : GenerationState.GenerateClassFilter() {
                    override fun shouldAnnotateClass(processingClassOrObject: KtClassOrObject): Boolean = true
                    
                    override fun shouldGenerateClass(processingClassOrObject: KtClassOrObject): Boolean {
                        val internalName = KotlinLightClassManager.getInternalName(processingClassOrObject)
                        return checkByInternalName(internalName, requestedClassName)
                    }
                    
                    override fun shouldGeneratePackagePart(jetFile: KtFile): Boolean {
                        val internalName = JvmFileClassUtil.getFileClassInternalName(jetFile)
                        return checkByInternalName(internalName, requestedClassName)
                    }
                    
                    override fun shouldGenerateScript(script: KtScript): Boolean = false

                    override fun shouldGenerateClassMembers(processingClassOrObject: KtClassOrObject): Boolean {
						return shouldGenerateClass(processingClassOrObject) ||
								processingClassOrObject.hasModifier(KtTokens.COMPANION_KEYWORD)
					}
			}).build()
        
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