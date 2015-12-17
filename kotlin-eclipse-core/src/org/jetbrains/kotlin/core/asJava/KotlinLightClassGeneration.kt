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

object KotlinLightClassGeneration {
    fun updateLightClasses(javaProject: IJavaProject, affectedFiles: Set<IFile>) {
        if (!KotlinJavaManager.hasLinkedKotlinBinFolder(javaProject)) return
        
        KotlinLightClassManager.getInstance(javaProject).computeLightClassesSources()
        KotlinLightClassManager.getInstance(javaProject).updateLightClasses(affectedFiles)
    }
    
    fun buildLightClasses(analysisResult: AnalysisResult, javaProject: IJavaProject, jetFiles: List<KtFile>) : GenerationState {
        val state = GenerationState(
                KotlinEnvironment.getEnvironment(javaProject).project,
                LightClassBuilderFactory(),
                analysisResult.moduleDescriptor,
                analysisResult.bindingContext,
                jetFiles)
        
        KotlinCodegenFacade.compileCorrectFiles(state) { exception, fileUrl -> Unit }
        
        return state
    }
}