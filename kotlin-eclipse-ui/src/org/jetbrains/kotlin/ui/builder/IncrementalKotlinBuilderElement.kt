package org.jetbrains.kotlin.ui.builder

import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.IResourceDelta
import org.eclipse.core.resources.IncrementalProjectBuilder
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.JavaCore
import org.jetbrains.kotlin.core.compiler.KotlinCompilerResult
import org.jetbrains.kotlin.core.compiler.KotlinCompilerUtils
import org.jetbrains.kotlin.ui.launch.removeKotlinConsoles

class IncrementalKotlinBuilderElement : BaseKotlinBuilderElement() {

    override fun build(project: IProject, delta: IResourceDelta?, kind: Int) {
        val javaProject = JavaCore.create(project)

        if (kind == IncrementalProjectBuilder.FULL_BUILD) {
            makeClean(javaProject)
            return
        }

        removeKotlinConsoles(javaProject)
        compileKotlinFilesIncrementally(javaProject)

        postBuild(delta, javaProject)
    }

    private fun compileKotlinFilesIncrementally(javaProject: IJavaProject) {
        val compilerResult: KotlinCompilerResult = KotlinCompilerUtils.compileProjectIncrementally(javaProject)
        if (!compilerResult.compiledCorrectly()) {
            KotlinCompilerUtils.handleCompilerOutput(KotlinCompilerUtils.CompilerOutputWithProject(compilerResult.compilerOutput, javaProject))
        }
    }
}