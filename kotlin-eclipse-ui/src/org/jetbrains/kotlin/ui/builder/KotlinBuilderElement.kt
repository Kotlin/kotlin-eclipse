package org.jetbrains.kotlin.ui.builder

import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.IResourceDelta
import org.eclipse.core.resources.IncrementalProjectBuilder.FULL_BUILD
import org.eclipse.core.runtime.Status
import org.eclipse.core.runtime.jobs.Job
import org.eclipse.debug.core.model.LaunchConfigurationDelegate
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.JavaCore
import org.jetbrains.kotlin.core.asJava.KotlinLightClassGeneration
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.core.compiler.KotlinCompilerResult
import org.jetbrains.kotlin.core.compiler.KotlinCompilerUtils
import org.jetbrains.kotlin.core.model.runJob
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer
import org.jetbrains.kotlin.ui.KotlinPluginUpdater

class KotlinBuilderElement : BaseKotlinBuilderElement() {

    override fun build(project: IProject, delta: IResourceDelta?, kind: Int): Array<IProject>? {
        val javaProject = JavaCore.create(project)
        if (isBuildingForLaunch()) {
            compileKotlinFiles(javaProject)
            return null
        }

        if (kind == FULL_BUILD) {
            makeClean(javaProject)
            return null
        }

        val allAffectedFiles = if (delta != null) getAllAffectedFiles(delta) else emptySet()

        if (allAffectedFiles.isNotEmpty()) {
            if (isAllFilesApplicableForFilters(allAffectedFiles, javaProject)) {
                return null
            }
        }

        val kotlinAffectedFiles =
            allAffectedFiles
                .filter { KotlinPsiManager.isKotlinSourceFile(it, javaProject) }
                .toSet()

        val existingAffectedFiles = kotlinAffectedFiles.filter { it.exists() }

        commitFiles(existingAffectedFiles)

        KotlinLightClassGeneration.updateLightClasses(javaProject.project, kotlinAffectedFiles)
        if (kotlinAffectedFiles.isNotEmpty()) {

            runJob("Checking for update", Job.DECORATE) {
                KotlinPluginUpdater.kotlinFileEdited()
                Status.OK_STATUS
            }
        }

        val ktFiles = existingAffectedFiles.map { KotlinPsiManager.getParsedFile(it) }

        val analysisResultWithProvider = if (ktFiles.isEmpty())
            KotlinAnalyzer.analyzeProject(project)
        else
            KotlinAnalyzer.analyzeFiles(ktFiles)

        clearProblemAnnotationsFromOpenEditorsExcept(existingAffectedFiles)
        updateLineMarkers(analysisResultWithProvider.analysisResult.bindingContext.diagnostics, existingAffectedFiles)

        runCancellableAnalysisFor(javaProject) { analysisResult ->
            val projectFiles = KotlinPsiManager.getFilesByProject(javaProject.project)
            updateLineMarkers(
                analysisResult.bindingContext.diagnostics,
                (projectFiles - existingAffectedFiles).toList()
            )
        }

        return null
    }

    private fun isBuildingForLaunch(): Boolean {
        val launchDelegateFQName = LaunchConfigurationDelegate::class.java.canonicalName
        return Thread.currentThread().stackTrace.find {
            it.className == launchDelegateFQName
        } != null
    }

    private fun compileKotlinFiles(javaProject: IJavaProject) {
        val compilerResult: KotlinCompilerResult = KotlinCompilerUtils.compileWholeProject(javaProject)
        if (!compilerResult.compiledCorrectly()) {
            KotlinCompilerUtils.handleCompilerOutput(compilerResult.compilerOutput)
        }
    }
}