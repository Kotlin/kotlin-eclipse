package org.jetbrains.kotlin.core.compiler

import com.intellij.openapi.util.Disposer
import org.eclipse.jdt.core.IJavaProject
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.EXCEPTION
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.core.launch.CompilerOutputData
import org.jetbrains.kotlin.core.launch.CompilerOutputParser
import org.jetbrains.kotlin.core.launch.KotlinCLICompiler
import org.jetbrains.kotlin.core.model.KOTLIN_COMPILER_PATH
import org.jetbrains.kotlin.core.model.KotlinEnvironment
import org.jetbrains.kotlin.core.preferences.CompilerPlugin
import org.jetbrains.kotlin.core.utils.ProjectUtils
import org.jetbrains.kotlin.incremental.makeIncrementally
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.io.Reader
import java.io.StringReader

object KotlinCompiler {

    private fun compileKotlinFiles(
        javaProject: IJavaProject,
        compilation: (IJavaProject, File, List<File>) -> KotlinCompilerResult
    ): KotlinCompilerResult =
        ProjectUtils.getSrcOutDirectories(javaProject)
            .groupingBy { it.second }.fold(mutableListOf<File>()) { list, key ->
                list.apply { add(key.first) }
            }.map { (out, sources) ->
                compilation(javaProject, out, sources)
            }.fold(KotlinCompilerResult(true, CompilerOutputData())) { previous, current ->
                KotlinCompilerResult(previous.result and current.result, CompilerOutputData().apply {
                    previous.compilerOutput.list.union(current.compilerOutput.list).forEach {
                        add(it.messageSeverity, it.message, it.messageLocation)
                    }
                })
            }

    @JvmStatic
    fun compileKotlinFiles(javaProject: IJavaProject): KotlinCompilerResult =
        compileKotlinFiles(javaProject) { project, path, sources ->
            execKotlinCompiler(configureCompilerArguments(project, path.absolutePath, sources))
        }

    @JvmStatic
    fun compileIncrementallyFiles(
        javaProject: IJavaProject
    ): KotlinCompilerResult =
        compileKotlinFiles(javaProject) { project, path, sources ->
            execIncrementalKotlinCompiler(project, path.absoluteFile, sources)
        }

    private fun execIncrementalKotlinCompiler(
        javaProject: IJavaProject,
        outputDir: File,
        sourceDirs: List<File>
    ): KotlinCompilerResult {
        val arguments = getCompilerArguments(javaProject, outputDir)
        val messageCollector = CompilerMessageCollector()
        val disposable = Disposer.newDisposable("Incremental compilation")
        val config = CompilerConfiguration().apply {
            put(JVMConfigurationKeys.NO_JDK, true)
            put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
            put(CLIConfigurationKeys.INTELLIJ_PLUGIN_ROOT, KOTLIN_COMPILER_PATH)
        }
        KotlinCoreEnvironment.getOrCreateApplicationEnvironmentForProduction(disposable, config)
        var cacheDir = File("${outputDir.parentFile.absolutePath}/cache").also { it.mkdirs() }
        makeIncrementally(cacheDir, sourceDirs, arguments, messageCollector)
        return messageCollector.getCompilerResult()
    }

    private fun execKotlinCompiler(arguments: Array<String>): KotlinCompilerResult = with(ByteArrayOutputStream()) {
        KotlinCLICompiler.doMain(K2JVMCompiler(), PrintStream(this), arguments)
        parseCompilerOutput(BufferedReader(StringReader(this.toString())))
    }

    private fun getCompilerArguments(javaProject: IJavaProject, outputDir: File) = K2JVMCompilerArguments().apply {
        val kotlinProperties =
            KotlinEnvironment.getEnvironment(javaProject.project).compilerProperties

        kotlinHome = ProjectUtils.ktHome
        destination = outputDir.absolutePath
        moduleName = "kotlin-eclipse-plugin"

        val jdkUndefined = kotlinProperties.isJDKHomUndefined()
        kotlinProperties.jdkHome?.takeUnless { jdkUndefined }?.let { jdkHomePath ->
            jdkHome = jdkHomePath
        } ?: {
            noJdk = true
        }()

        noStdlib = true
        jvmTarget = kotlinProperties.jvmTarget.description
        intellijPluginRoot = KOTLIN_COMPILER_PATH
        languageVersion = kotlinProperties.languageVersion.versionString
        apiVersion = kotlinProperties.apiVersion.versionString

        val pluginClasspathsList = mutableListOf<String>()
        val pluginOptionsList = mutableListOf<String>()

        kotlinProperties.compilerPlugins.entries.forEach { plugin ->
            plugin.jarPath?.takeIf { plugin.active }?.let { jarPath ->
                pluginClasspathsList.add(jarPath.replace("\$KOTLIN_HOME", ProjectUtils.ktHome))
                plugin.args.forEach { arg ->
                    pluginOptionsList.add("plugin: $arg")
                }
            }
        }

        pluginClasspaths = pluginClasspathsList.toTypedArray()
        pluginOptions = pluginOptionsList.toTypedArray()

        classpath = ProjectUtils.collectClasspathWithDependenciesForLaunch(javaProject, jdkUndefined)
            .joinToString(separator = System.getProperty("path.separator")) { it.absolutePath }


    }

    private fun configureCompilerArguments(
        javaProject: IJavaProject, outputDir: String, sourceDirs: List<File>
    ): Array<String> = with(mutableListOf<String>()) {
        val kotlinProperties =
            KotlinEnvironment.getEnvironment(javaProject.project).compilerProperties

        add("-kotlin-home")
        add(ProjectUtils.ktHome)

        val jdkUndefined = kotlinProperties.isJDKHomUndefined()
        kotlinProperties.jdkHome?.takeUnless { jdkUndefined }?.let { jdkHomePath ->
            add("-jdk-home")
            add(jdkHomePath)
        } ?: add("-no-jdk")


        add("-no-stdlib") // Because we add runtime into the classpath

        add("-jvm-target")
        add(kotlinProperties.jvmTarget.description)

        add("-language-version")
        add(kotlinProperties.languageVersion.versionString)

        add("-api-version")
        add(kotlinProperties.apiVersion.versionString)

        kotlinProperties.compilerPlugins.entries.forEach { plugin ->
            addAll(configurePlugin(plugin))
        }

        kotlinProperties.compilerFlags?.takeUnless { it.isBlank() }?.split("\\s+".toRegex())?.let {
            addAll(it)
        }

        add("-classpath")
        ProjectUtils.collectClasspathWithDependenciesForLaunch(javaProject, jdkUndefined)
            .joinToString(separator = System.getProperty("path.separator")) { it.absolutePath }
            .let { add(it) }

        add("-d")
        add(outputDir)

        addAll(sourceDirs.map {
            it.absolutePath
        })

        toTypedArray()
    }

    private fun configurePlugin(plugin: CompilerPlugin): Collection<String> = mutableListOf<String>().apply {
        plugin.jarPath?.takeIf { plugin.active }?.let { jarPath ->
            add("-Xplugin=${jarPath.replace("\$KOTLIN_HOME", ProjectUtils.ktHome)}")
            plugin.args.forEach { arg ->
                add("-P")
                add("plugin: $arg")
            }
        }
    }

    private class CompilerMessageCollector : MessageCollector {
        var hasErrors = false
        val severities: MutableList<CompilerMessageSeverity> = mutableListOf()
        val compilerOutput = CompilerOutputData()

        override fun report(
            severity: CompilerMessageSeverity,
            message: String,
            location: CompilerMessageSourceLocation?
        ) {
            hasErrors == hasErrors || severity.isError
            severities.add(severity)
            if (location != null) {
                val messageLocation = CompilerMessageLocation.create(location.path, location.line, location.column, location.lineContent)
                compilerOutput.add(severity, message, messageLocation)
            } else {
                compilerOutput.add(severity, message, null)
            }
        }

        override fun hasErrors(): Boolean = hasErrors

        override fun clear() {
            hasErrors = false
        }

        fun getCompilerResult(): KotlinCompilerResult =
            KotlinCompilerResult(severities.firstOrNull { it == ERROR || it == EXCEPTION } == null, compilerOutput)
    }

    private fun parseCompilerOutput(reader: Reader): KotlinCompilerResult {
        val messageCollector = CompilerMessageCollector()

        CompilerOutputParser.parseCompilerMessagesFromReader(messageCollector, reader)

        return messageCollector.getCompilerResult()
    }
}

class KotlinCompilerResult(val result: Boolean, val compilerOutput: CompilerOutputData) {

    fun compiledCorrectly() = result
}