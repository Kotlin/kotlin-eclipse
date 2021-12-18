package org.jetbrains.kotlin.ui.builder

import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.IResourceDelta
import org.eclipse.core.resources.IncrementalProjectBuilder
import org.eclipse.core.runtime.Status
import org.eclipse.core.runtime.jobs.Job
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

class IncrementalKotlinBuilderElement : BaseKotlinBuilderElement() {

    override fun build(project: IProject, delta: IResourceDelta?, kind: Int): Array<IProject>? {
        val javaProject = JavaCore.create(project)

        if (kind == IncrementalProjectBuilder.FULL_BUILD) {
            makeClean(javaProject)
            return null
        }

        removeKotlinConsoles(javaProject)
        compileKotlinFilesIncrementally(javaProject)

        postBuild(delta, javaProject)

        return null
    }

    private fun compileKotlinFilesIncrementally(javaProject: IJavaProject) {
        val compilerResult: KotlinCompilerResult = KotlinCompilerUtils.compileProjectIncrementally(javaProject)
        if (!compilerResult.compiledCorrectly()) {
            KotlinCompilerUtils.handleCompilerOutput(KotlinCompilerUtils.CompilerOutputWithProject(compilerResult.compilerOutput, javaProject))
        }
    }
}