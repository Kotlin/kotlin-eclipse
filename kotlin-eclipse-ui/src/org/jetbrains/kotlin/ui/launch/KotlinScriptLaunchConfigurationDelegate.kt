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
package org.jetbrains.kotlin.ui.launch

import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.Path
import org.eclipse.debug.core.ILaunch
import org.eclipse.debug.core.ILaunchConfiguration
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate
import org.eclipse.jdt.launching.ExecutionArguments
import org.eclipse.jdt.launching.VMRunnerConfiguration
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.core.compiler.KotlinCompiler
import org.jetbrains.kotlin.core.model.EclipseScriptDefinitionProvider
import org.jetbrains.kotlin.core.model.KOTLIN_COMPILER_PATH
import org.jetbrains.kotlin.core.model.KotlinScriptEnvironment
import org.jetbrains.kotlin.core.utils.ProjectUtils
import org.jetbrains.kotlin.core.utils.asFile
import java.io.File
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.baseClass

class KotlinScriptLaunchConfigurationDelegate : AbstractJavaLaunchConfigurationDelegate() {
    companion object {
        private val ideDepenedneciesPath = ProjectUtils.buildLibPath("ide-dependencies")
        private val compilerMainClass = K2JVMCompiler::class.java.name

        private val classpathForCompiler =
            listOf(KOTLIN_COMPILER_PATH, ideDepenedneciesPath)
                .map { File(it).absolutePath }
                .toTypedArray()
    }

    override fun launch(configuration: ILaunchConfiguration, mode: String, launch: ILaunch, monitor: IProgressMonitor) {
        monitor.beginTask("${configuration.name}...", 5)
        try {
            monitor.subTask("Verifying launch attributes...")

            val scriptFile = getScriptFile(configuration) ?: return
            if (!scriptFile.exists()) return

            monitor.subTask("Building project...")
            val javaProject = JavaCore.create(scriptFile.project)
            KotlinCompiler.INSTANCE.compileKotlinFiles(javaProject)

            if (monitor.isCanceled) return
            monitor.worked(3)

            val runConfig = createRunnerConfiguration(configuration, scriptFile, javaProject)

            if (monitor.isCanceled) return
            monitor.worked(1)

            val runner = getVMRunner(configuration, mode)
            runner.run(runConfig, launch, monitor)

            if (monitor.isCanceled) return
        } finally {
            monitor.done()
        }
    }

    private fun createRunnerConfiguration(
        configuration: ILaunchConfiguration,
        scriptFile: IFile,
        javaProject: IJavaProject
    ): VMRunnerConfiguration {
        val workingDir = verifyWorkingDirectory(configuration)?.absolutePath

        val programArgs = getProgramArguments(configuration)
        val vmArgs = getVMArguments(configuration)
        val executionArgs = ExecutionArguments(vmArgs, programArgs)

        val environmentVars = getEnvironment(configuration)

        val vmAttributesMap: MutableMap<String, Any>? = getVMSpecificAttributesMap(configuration)

        return VMRunnerConfiguration(compilerMainClass, classpathForCompiler + templateClasspath(scriptFile)).apply {
            programArguments = buildCompilerArguments(
                scriptFile,
                javaProject,
                executionArgs.programArgumentsArray
            ).toTypedArray()

            environment = environmentVars
            vmArguments = executionArgs.vmArgumentsArray
            workingDirectory = workingDir
            vmSpecificAttributesMap = vmAttributesMap

            bootClassPath = getBootpath(configuration)
        }
    }

    private fun templateClasspath(scriptFile: IFile): Array<String> =
        KotlinScriptEnvironment.getEnvironment(scriptFile).definitionClasspath.map { it.absolutePath }.toTypedArray()

    private fun getScriptFile(configuration: ILaunchConfiguration): IFile? =
        configuration.getAttribute(SCRIPT_FILE_PATH, null as String?)?.let { scriptFilePath ->
            ResourcesPlugin.getWorkspace().root.getFile(Path(scriptFilePath))
        }

    private fun buildCompilerArguments(
        scriptFile: IFile,
        javaProject: IJavaProject,
        programArguments: Array<String>
    ): List<String> =
        arrayListOf<String>().apply {
            val environment = KotlinScriptEnvironment.getEnvironment(scriptFile)

            val classpathEntries = environment.dependencies?.classpath?.toList().orEmpty()

            val pathSeparator = System.getProperty("path.separator")

            add("-kotlin-home")
            add(ProjectUtils.ktHome)

            add("-classpath")
            add(classpathEntries.joinToString(separator = pathSeparator))
            add("-script")
            add(scriptFile.location.toOSString())

            environment.definition
                ?.compilationConfiguration
                ?.get(ScriptCompilationConfiguration.baseClass)
                ?.typeName
                ?.also {
                    add("-script-templates")
                    add(it)
                }

            val formattedEnvironment = EclipseScriptDefinitionProvider.getEnvironment(scriptFile.asFile)
                .entries
                .joinToString(separator = ",") { (k, v) -> "$k=$v" }
            add("-Xscript-resolver-environment=$formattedEnvironment")

            addAll(programArguments)
        }
}