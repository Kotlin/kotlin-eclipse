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
import org.jetbrains.kotlin.core.compiler.KotlinCompiler
import org.jetbrains.kotlin.core.compiler.KotlinCompilerUtils
import org.jetbrains.kotlin.core.utils.ProjectUtils
import java.io.File
import org.jetbrains.kotlin.core.model.KOTLIN_COMPILER_PATH
import java.io.PrintStream

class KotlinScriptLaunchConfigurationDelegate : AbstractJavaLaunchConfigurationDelegate() {
    override fun launch(configuration: ILaunchConfiguration, mode: String, launch: ILaunch, monitor: IProgressMonitor) {
        monitor.beginTask("${configuration.name}...", 2)
        try {
            monitor.subTask("Verifying launch attributes...")
            
            val scriptFilePath = configuration.getAttribute(SCRIPT_FILE_PATH, null as String?) ?: return
            val scriptFile = ResourcesPlugin.getWorkspace().getRoot().getFile(Path(scriptFilePath))
            
            if (!scriptFile.exists()) return
            
            val mainTypeName = K2JVMCompiler::class.java.name
            val compilerClasspath = File(KOTLIN_COMPILER_PATH).absolutePath
            
            val runner = getVMRunner(configuration, mode)
            
            val workingDir = verifyWorkingDirectory(configuration)?.absolutePath
            
            val programArgs = getProgramArguments(configuration)
            val vmArgs = getVMArguments(configuration)
            val executionArgs = ExecutionArguments(vmArgs, programArgs)
            
            val environmentVars = getEnvironment(configuration);

            val vmAttributesMap = getVMSpecificAttributesMap(configuration)
            
            if (monitor.isCanceled) {
                return
            }
            
            monitor.worked(1)
            
            val runConfig = VMRunnerConfiguration(mainTypeName, arrayOf(compilerClasspath)).apply {
                setProgramArguments(
                        buildCompilerArguments(scriptFile, executionArgs.programArgumentsArray).toTypedArray());
                setEnvironment(environmentVars);
                setVMArguments(executionArgs.vmArgumentsArray);
                setWorkingDirectory(workingDir);
                setVMSpecificAttributesMap(vmAttributesMap);
                
                setBootClassPath(getBootpath(configuration));
            }
            
            if (monitor.isCanceled()) {
                return;
            }       
            
            monitor.worked(1)
            
            runner.run(runConfig, launch, monitor)
            
            if (monitor.isCanceled()) {
                return;
            }   
        } finally {
            monitor.done()
        }
        
    }
    
    private fun buildCompilerArguments(scriptFile: IFile, programArguments: Array<String>): List<String> {
        return arrayListOf<String>().apply {
            add("-kotlin-home")
            add(ProjectUtils.KT_HOME)
            
            add("-script")
            add(scriptFile.getLocation().toOSString())
            
            addAll(programArguments)
        }
    }
}