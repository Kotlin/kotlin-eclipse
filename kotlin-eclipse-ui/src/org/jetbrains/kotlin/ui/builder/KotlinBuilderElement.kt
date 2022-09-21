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
import org.jetbrains.kotlin.ui.launch.removeKotlinConsoles

class KotlinBuilderElement : BaseKotlinBuilderElement() {

    override fun build(project: IProject, delta: IResourceDelta?, kind: Int): Array<IProject>? {
        val javaProject = JavaCore.create(project)
        if (isBuildingForLaunch()) {
            removeKotlinConsoles(javaProject)
            compileKotlinFiles(javaProject)
            return null
        }

        if (kind == FULL_BUILD) {
            makeClean(javaProject)
            return null
        }

        postBuild(delta, javaProject)

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
            KotlinCompilerUtils.handleCompilerOutput(KotlinCompilerUtils.CompilerOutputWithProject(compilerResult.compilerOutput, javaProject))
        }
    }
}