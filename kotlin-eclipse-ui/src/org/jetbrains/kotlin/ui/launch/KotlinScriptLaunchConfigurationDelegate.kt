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
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate
import org.eclipse.jdt.launching.ExecutionArguments
import org.eclipse.jdt.launching.VMRunnerConfiguration
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.core.model.KOTLIN_COMPILER_PATH
import org.jetbrains.kotlin.core.utils.ProjectUtils
import java.io.File

class KotlinScriptLaunchConfigurationDelegate : AbstractJavaLaunchConfigurationDelegate() {
    companion object {
        private val compilerMainClass = K2JVMCompiler::class.java.name
        private val classpathForCompiler = arrayOf(File(KOTLIN_COMPILER_PATH).absolutePath)
    }
    
    override fun launch(configuration: ILaunchConfiguration, mode: String, launch: ILaunch, monitor: IProgressMonitor) {
        monitor.beginTask("${configuration.name}...", 2)
        try {
            monitor.subTask("Verifying launch attributes...")
            
            val runConfig = verifyAndCreateRunnerConfiguration(configuration) ?: return
            
            if (monitor.isCanceled()) {
                return;
            }       
            
            monitor.worked(1)
            
            val runner = getVMRunner(configuration, mode)
            runner.run(runConfig, launch, monitor)
            
            if (monitor.isCanceled()) {
                return;
            }   
        } finally {
            monitor.done()
        }
    }
    
    private fun verifyAndCreateRunnerConfiguration(configuration: ILaunchConfiguration): VMRunnerConfiguration? {
        val scriptFile = getScriptFile(configuration) ?: return null
        if (!scriptFile.exists()) return null

        val workingDir = verifyWorkingDirectory(configuration)?.absolutePath

        val programArgs = getProgramArguments(configuration)
        val vmArgs = getVMArguments(configuration)
        val executionArgs = ExecutionArguments(vmArgs, programArgs)

        val environmentVars = getEnvironment(configuration)

        val vmAttributesMap: MutableMap<String, Any>? = getVMSpecificAttributesMap(configuration)

        return VMRunnerConfiguration(compilerMainClass, classpathForCompiler).apply {
            setProgramArguments(buildCompilerArguments(
                    scriptFile,
                    executionArgs.programArgumentsArray).toTypedArray())

            setEnvironment(environmentVars)
            setVMArguments(executionArgs.vmArgumentsArray)
            setWorkingDirectory(workingDir);
            setVMSpecificAttributesMap(vmAttributesMap)

            setBootClassPath(getBootpath(configuration))
        }
    }
    
    
    
    private fun getScriptFile(configuration: ILaunchConfiguration): IFile? {
        return configuration.getAttribute(SCRIPT_FILE_PATH, null as String?)?.let { scriptFilePath ->
            ResourcesPlugin.getWorkspace().root.getFile(Path(scriptFilePath))
        }
    }
    
    private fun buildCompilerArguments(scriptFile: IFile, programArguments: Array<String>): List<String> {
        return arrayListOf<String>().apply {
            add("-kotlin-home")
            add(ProjectUtils.ktHome)
            
            add("-script")
            add(scriptFile.getLocation().toOSString())
            
            addAll(programArguments)
        }
    }
}