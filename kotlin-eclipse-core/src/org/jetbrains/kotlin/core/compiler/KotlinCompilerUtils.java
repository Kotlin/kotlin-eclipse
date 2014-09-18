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
 *******************************************************************************/
package org.jetbrains.kotlin.core.compiler;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.jdt.core.IJavaProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.core.Activator;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.compiler.KotlinCompiler.KotlinCompilerResult;
import org.jetbrains.kotlin.core.launch.CompilerOutputData;

public class KotlinCompilerUtils {
    @NotNull
    public static KotlinCompilerResult compileWholeProject(@NotNull IJavaProject javaProject) throws CoreException, InterruptedException, IOException {
        return KotlinCompiler.INSTANCE.compileKotlinFiles(
                KotlinPsiManager.INSTANCE.getFilesByProject(javaProject.getProject()), 
                javaProject);
    }
    
    public static void handleCompilerOutput(@NotNull CompilerOutputData compilerOutput) throws CoreException {
        IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, 1, "", null);
        IStatusHandler handler = DebugPlugin.getDefault().getStatusHandler(status);
        
        if (handler != null) {
            handler.handleStatus(status, compilerOutput);
        }
    }
}
