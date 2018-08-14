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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation;
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler;
import org.jetbrains.kotlin.core.launch.CompilerOutputData;
import org.jetbrains.kotlin.core.launch.CompilerOutputElement;
import org.jetbrains.kotlin.core.launch.CompilerOutputParser;
import org.jetbrains.kotlin.core.launch.KotlinCLICompiler;
import org.jetbrains.kotlin.core.model.KotlinEnvironment;
import org.jetbrains.kotlin.core.preferences.CompilerPlugin;
import org.jetbrains.kotlin.core.preferences.KotlinProperties;
import org.jetbrains.kotlin.core.utils.ProjectUtils;

import kotlin.Pair;
import kotlin.text.StringsKt;
import org.jetbrains.kotlin.utils.PathUtil;

public class KotlinCompiler {
    public final static KotlinCompiler INSTANCE = new KotlinCompiler();

    private KotlinCompiler() {
    }

    @NotNull
    public KotlinCompilerResult compileKotlinFiles(@NotNull IJavaProject javaProject) throws CoreException {
        return ProjectUtils.getSrcOutDirectories(javaProject)
            .stream()
            .collect(Collectors.groupingBy(Pair::component2))
            .entrySet()
            .stream()
            .map(outSrcOut -> {
                File out = outSrcOut.getKey();
                List<File> srcs = outSrcOut.getValue()
                    .stream()
                    .map(pair -> pair.component1())
                    .collect(Collectors.toList());
                return new Pair<File, List<File>>(out, srcs);
            })
            .map(outSrcs -> {
                File out = outSrcs.component1();
                List<File> srcs = outSrcs.component2();
                try {
                    String[] arguments = configureCompilerArguments(javaProject, out.getAbsolutePath(), srcs);
                    return execKotlinCompiler(arguments);
                } catch (CoreException ce) {
                    throw new RuntimeException(ce);
                }
            })
            .reduce(new KotlinCompilerResult(true, new CompilerOutputData()), (leftResult, rightResult) -> {
                CompilerOutputData mergedData = new CompilerOutputData();
                List<CompilerOutputElement> mergedList = leftResult.compilerOutput.getList();
                mergedList.addAll(rightResult.compilerOutput.getList());
                mergedList.forEach(outElement -> {
                    mergedData.add(outElement.getMessageSeverity(), outElement.getMessage(), outElement.getMessageLocation());
                });
                return new KotlinCompilerResult(leftResult.result && rightResult.result, mergedData);
            });
    }

    public KotlinCompilerResult execKotlinCompiler(@NotNull String[] arguments) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(outputStream);

        KotlinCLICompiler.doMain(new K2JVMCompiler(), out, arguments);

        BufferedReader reader = new BufferedReader(new StringReader(outputStream.toString()));
        return parseCompilerOutput(reader);
    }

    private String[] configureCompilerArguments(@NotNull IJavaProject javaProject, @NotNull String outputDir,
            @NotNull List<File> sourceDirs) throws CoreException {
        KotlinProperties kotlinProperties =
                KotlinEnvironment.getEnvironment(javaProject.getProject()).getCompilerProperties();

        List<String> command = new ArrayList<>();
        command.add("-kotlin-home");
        command.add(ProjectUtils.getKtHome());

        boolean jdkHomeUndefined = kotlinProperties.isJDKHomUndefined();
        if (jdkHomeUndefined) {
            command.add("-no-jdk");
        } else {
            command.add("-jdk-home");
            command.add(kotlinProperties.getJdkHome());
        }
        command.add("-no-stdlib"); // Because we add runtime into the classpath

        command.add("-jvm-target");
        command.add(kotlinProperties.getJvmTarget().getDescription());

        command.add("-language-version");
        command.add(kotlinProperties.getLanguageVersion().getVersionString());

        command.add("-api-version");
        command.add(kotlinProperties.getApiVersion().getVersionString());

        for (CompilerPlugin plugin : kotlinProperties.getCompilerPlugins().getEntries()) {
            command.addAll(configurePlugin(plugin));
        }
        command.add(configureScriptingPlugin());

        StringBuilder classPath = new StringBuilder();
        String pathSeparator = System.getProperty("path.separator");

        for (File file : ProjectUtils.collectClasspathWithDependenciesForLaunch(javaProject, jdkHomeUndefined)) {
            classPath.append(file.getAbsolutePath()).append(pathSeparator);
        }

        String additionalFlags = kotlinProperties.getCompilerFlags();
        if (additionalFlags != null && !StringsKt.isBlank(additionalFlags)) {
            for (String flag : additionalFlags.split("\\s+")) {
                command.add(flag);
            }
        }

        command.add("-classpath");
        command.add(classPath.toString());

        command.add("-d");
        command.add(outputDir);

        for (File srcDirectory : sourceDirs) {
            command.add(srcDirectory.getAbsolutePath());
        }

        return command.toArray(new String[0]);
    }

    private Collection<String> configurePlugin(CompilerPlugin plugin) {
        List<String> result = new ArrayList<>();
        String jarPath = plugin.getJarPath();
        if (plugin.getActive() && jarPath != null) {
            String replacedPath = jarPath.replace("$KOTLIN_HOME", ProjectUtils.getKtHome());
            result.add("-Xplugin=" + replacedPath);

            for (String arg : plugin.getArgs()) {
                result.add("-P");
                result.add("plugin:" + arg);
            }
        }
        return result;
    }

    private String configureScriptingPlugin() {
        return "-Xplugin=" + ProjectUtils.buildLibPath(PathUtil.KOTLIN_SCRIPTING_COMPILER_PLUGIN_NAME);
    }

    @NotNull
    private KotlinCompilerResult parseCompilerOutput(Reader reader) {
        final CompilerOutputData compilerOutput = new CompilerOutputData();

        final List<CompilerMessageSeverity> severities = new ArrayList<CompilerMessageSeverity>();
        CompilerOutputParser.parseCompilerMessagesFromReader(new MessageCollector() {
            private boolean hasErrors = false;

            @Override
            public void report(@NotNull CompilerMessageSeverity messageSeverity, @NotNull String message,
                    @Nullable CompilerMessageLocation messageLocation) {
                hasErrors = hasErrors || messageSeverity.isError();
                severities.add(messageSeverity);
                compilerOutput.add(messageSeverity, message, messageLocation);
            }

            @Override
            public boolean hasErrors() {
                return hasErrors;
            }

            @Override
            public void clear() {
                hasErrors = false;

            }
        }, reader);

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
