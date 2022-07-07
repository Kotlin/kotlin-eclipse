/*******************************************************************************
 * Copyright 2010-2014 JetBrains s.r.o.
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
 */
package org.jetbrains.kotlin.core.compiler

import org.jetbrains.kotlin.core.compiler.KotlinCompiler.compileKotlinFiles
import org.jetbrains.kotlin.core.compiler.KotlinCompiler.compileIncrementallyFiles
import org.eclipse.core.runtime.CoreException
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.Status
import org.eclipse.debug.core.IStatusHandler
import org.eclipse.debug.core.DebugPlugin
import org.jetbrains.kotlin.core.Activator
import org.jetbrains.kotlin.core.launch.CompilerOutputData

object KotlinCompilerUtils {

    fun compileWholeProject(javaProject: IJavaProject): KotlinCompilerResult = compileKotlinFiles(javaProject)

    fun compileProjectIncrementally(javaProject: IJavaProject): KotlinCompilerResult =
            compileIncrementallyFiles(javaProject)

    fun handleCompilerOutput(compilerOutput: CompilerOutputWithProject) {
        val status: IStatus = Status(IStatus.ERROR, Activator.PLUGIN_ID, 1, "", null)
        val handler = DebugPlugin.getDefault().getStatusHandler(status)
        handler?.handleStatus(status, compilerOutput)
    }

    data class CompilerOutputWithProject(val data: CompilerOutputData, val project: IJavaProject)
}