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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation;
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler;
import org.jetbrains.kotlin.core.launch.CompilerOutputData;
import org.jetbrains.kotlin.core.launch.CompilerOutputParser;
import org.jetbrains.kotlin.core.launch.KotlinCLICompiler;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.utils.ProjectUtils;

public class KotlinCompiler {
    public final static KotlinCompiler INSTANCE = new KotlinCompiler();
    
    private KotlinCompiler() {
    }
    
    @NotNull
    public KotlinCompilerResult compileKotlinFiles(@NotNull IJavaProject javaProject) 
            throws CoreException {
        IFolder outputFolder = ProjectUtils.getOutputFolder(javaProject);
        if (outputFolder == null) {
            KotlinLogger.logError("There is no output folder for project: " + javaProject, null);
            return KotlinCompilerResult.EMPTY;
        }
        
        String[] arguments = configureCompilerArguments(javaProject, outputFolder.getLocation().toOSString());
        
        return execKotlinCompiler(arguments);
    }
    
    private KotlinCompilerResult execKotlinCompiler(@NotNull String[] arguments) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(outputStream);
        
        KotlinCLICompiler.doMain(new K2JVMCompiler(), out, arguments);
        
        BufferedReader reader = new BufferedReader(new StringReader(outputStream.toString()));
        return parseCompilerOutput(reader);
    }
    
    private String[] configureCompilerArguments(@NotNull IJavaProject javaProject, @NotNull String outputDir) throws CoreException {
        List<String> command = new ArrayList<String>();
        command.add("-kotlin-home");
        command.add(ProjectUtils.KT_HOME);
        command.add("-no-jdk-annotations"); // TODO: remove this option when external annotation support will be added
        command.add("-no-jdk");
        command.add("-no-stdlib"); // Because we add runtime into the classpath
        
        StringBuilder classPath = new StringBuilder();
        String pathSeparator = System.getProperty("path.separator");
        
        for (File file : ProjectUtils.collectClasspathWithDependenciesForLaunch(javaProject)) {
            classPath.append(file.getAbsolutePath()).append(pathSeparator);
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
    private KotlinCompilerResult parseCompilerOutput(Reader reader) {
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
                reader);
        
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
        public static KotlinCompilerResult EMPTY = new KotlinCompilerResult(false, new CompilerOutputData());
        
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
