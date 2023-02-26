package org.jetbrains.kotlin.ui.builder

import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.IResourceDelta
import org.eclipse.core.resources.IncrementalProjectBuilder.FULL_BUILD
import org.eclipse.debug.core.model.LaunchConfigurationDelegate
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.JavaCore
import org.jetbrains.kotlin.core.compiler.KotlinCompilerResult
import org.jetbrains.kotlin.core.compiler.KotlinCompilerUtils
import org.jetbrains.kotlin.core.model.KotlinEnvironment
import org.jetbrains.kotlin.ui.launch.removeKotlinConsoles

class KotlinBuilderElement : BaseKotlinBuilderElement() {

    override fun build(project: IProject, delta: IResourceDelta?, kind: Int) {
        val javaProject = JavaCore.create(project)
        val isBuildRealAlways = KotlinEnvironment.getEnvironment(project).buildingProperties.alwaysRealBuild
        if (isBuildRealAlways || isBuildingForLaunch()) {
            removeKotlinConsoles(javaProject)
            compileKotlinFiles(javaProject)

            if(!isBuildRealAlways) {
                return
            }
        }

        if (kind == FULL_BUILD) {
            makeClean(javaProject)
            return
        }

        postBuild(delta, javaProject)
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