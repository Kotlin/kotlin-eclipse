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
package org.jetbrains.kotlin.core.launch;

import java.io.File;
import java.io.PrintStream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.common.CLICompiler;
import org.jetbrains.kotlin.cli.common.ExitCode;
import org.jetbrains.kotlin.cli.jvm.compiler.CompileEnvironmentException;
import org.jetbrains.kotlin.cli.jvm.compiler.CompilerJarLocator;
import org.jetbrains.kotlin.config.Services;
import org.jetbrains.kotlin.config.Services.Builder;
import org.jetbrains.kotlin.core.utils.ProjectUtils;

public class KotlinCLICompiler {
    public static int doMain(@NotNull CLICompiler<?> compiler, @NotNull PrintStream errorStream, @NotNull String[] args) {
        System.setProperty("java.awt.headless", "true");
        ExitCode exitCode = doMainNoExitWithHtmlOutput(compiler, errorStream, args);
        
        return exitCode.getCode();
    }
    
    private static ExitCode doMainNoExitWithHtmlOutput(
            @NotNull CLICompiler<?> compiler, 
            @NotNull PrintStream errorStream, 
            @NotNull String[] args) {
        try {
            Builder builder = new Services.Builder();
            builder.register(CompilerJarLocator.class, new CompilerJarLocator() {
                @NotNull
                @Override
                public File getCompilerJar() {
                    return new File(ProjectUtils.buildLibPath("kotlin-compiler"));
                }
            });
            
            return compiler.execAndOutputXml(errorStream, builder.build(), args);
        } catch (CompileEnvironmentException e) {
            errorStream.println(e.getMessage());
            return ExitCode.INTERNAL_ERROR;
        }
    }
}
