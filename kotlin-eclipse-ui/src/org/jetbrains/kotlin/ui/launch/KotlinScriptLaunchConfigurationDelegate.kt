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
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate
import org.jetbrains.kotlin.core.compiler.KotlinCompiler
import org.jetbrains.kotlin.core.utils.ProjectUtils
import java.io.PrintStream

class KotlinScriptLaunchConfigurationDelegate : ILaunchConfigurationDelegate {
    override fun launch(configuration: ILaunchConfiguration, mode: String, launch: ILaunch, monitor: IProgressMonitor) {
        val scriptFilePath = configuration.getAttribute(SCRIPT_FILE_PATH, null as String?) ?: return
        val scriptFile = ResourcesPlugin.getWorkspace().getRoot().getFile(Path(scriptFilePath))
        
        if (!scriptFile.exists()) return
        
        execKotlinCompiler(buildCompilerArguments(scriptFile))
    }
    
    private fun execKotlinCompiler(arguments: List<String>) {
        val systemOut = System.out
        try {
            System.setOut(PrintStream(createCleanKotlinConsole().newOutputStream()))
            
            KotlinCompiler.INSTANCE.execKotlinCompiler(arguments.toTypedArray())
        } finally {
            System.setOut(systemOut)
        }
    }
    
    private fun buildCompilerArguments(scriptFile: IFile): List<String> {
        return arrayListOf<String>().apply {
            add("-kotlin-home")
            add(ProjectUtils.KT_HOME)
            
            add("-script")
            add(scriptFile.getLocation().toOSString())
        }
    }
}   