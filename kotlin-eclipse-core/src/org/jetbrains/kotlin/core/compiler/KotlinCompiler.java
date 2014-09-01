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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.cli.common.messages.CompilerMessageLocation;
import org.jetbrains.jet.cli.common.messages.CompilerMessageSeverity;
import org.jetbrains.jet.cli.common.messages.MessageCollector;
import org.jetbrains.jet.cli.jvm.K2JVMCompiler;
import org.jetbrains.kotlin.core.launch.CompilerOutputData;
import org.jetbrains.kotlin.core.launch.CompilerOutputParser;
import org.jetbrains.kotlin.core.utils.ProjectUtils;

public class KotlinCompiler {
    public final static KotlinCompiler INSTANCE = new KotlinCompiler();
    
    private final static String KT_COMPILER_PATH = ProjectUtils.buildLibPath("kotlin-compiler");
    
    private KotlinCompiler() {
    }
    
    @NotNull
    public KotlinCompilerResult compileKotlinFiles(@NotNull List<IFile> files, @NotNull IJavaProject javaProject, @NotNull String outputDir) 
            throws CoreException, InterruptedException, IOException {
        String[] command = configureBuildCommand(javaProject, outputDir);
        
        Process buildProcess = Runtime.getRuntime().exec(command);
        KotlinCompilerResult compilerResult = parseCompilerOutput(buildProcess.getErrorStream());
        
        buildProcess.waitFor();
        
        return compilerResult;
    }
    
    private String[] configureBuildCommand(@NotNull IJavaProject javaProject, @NotNull String outputDir) throws CoreException {
        List<String> command = new ArrayList<String>();
        command.add("java");
        command.add("-cp");
        command.add(KT_COMPILER_PATH);
        command.add(K2JVMCompiler.class.getCanonicalName());
        command.add("-kotlinHome");
        command.add(ProjectUtils.KT_HOME);
        command.add("-tags");
        
        StringBuilder classPath = new StringBuilder();
        String pathSeparator = System.getProperty("path.separator");
        
        for (File srcDirectory : ProjectUtils.getSrcDirectories(javaProject)) {
            classPath.append(srcDirectory.getAbsolutePath()).append(pathSeparator);
        }
        
        for (File libDirectory : ProjectUtils.getLibDirectories(javaProject)) {
            classPath.append(libDirectory.getAbsolutePath()).append(pathSeparator);
        }
        
        command.add("-classpath");
        command.add(classPath.toString());
        
        command.add("-d");
        command.add(outputDir);
        
        for (File srcDirectory : ProjectUtils.getSrcDirectories(javaProject)) {
            command.add(srcDirectory.getAbsolutePath());
        }
        
        return command.toArray(new String[command.size()]);
    }

    @NotNull
    private KotlinCompilerResult parseCompilerOutput(InputStream inputStream) {
        final CompilerOutputData compilerOutput = new CompilerOutputData(); 
        
        final List<CompilerMessageSeverity> severities = new ArrayList<CompilerMessageSeverity>();
        CompilerOutputParser.parseCompilerMessagesFromReader(
                new MessageCollector() {
                    @Override
                    public void report(@NotNull CompilerMessageSeverity messageSeverity, @NotNull String message,
                            @NotNull CompilerMessageLocation messageLocation) {
                        severities.add(messageSeverity);
                        compilerOutput.add(messageSeverity, message, messageLocation);
                    }
                },
                new InputStreamReader(inputStream));
        
        boolean result = true;
        for (CompilerMessageSeverity severity : severities) {
            if (severity.equals(CompilerMessageSeverity.ERROR) || severity.equals(CompilerMessageSeverity.EXCEPTION)) {
                result = false;
                break;
            }
        }
        
        return new KotlinCompilerResult(result, compilerOutput);
    }
    
    public static class KotlinCompilerResult {
        private final boolean result;
        private final CompilerOutputData compilerOutput;
        
        private KotlinCompilerResult(boolean result, @NotNull CompilerOutputData compilerOutput) {
            this.result = result;
            this.compilerOutput = compilerOutput;
        }
        
        public boolean compiledCorrectly() {
            return result;
        }
        
        @NotNull
        public CompilerOutputData getCompilerOutput() {
            return compilerOutput;
        }
    }
}
