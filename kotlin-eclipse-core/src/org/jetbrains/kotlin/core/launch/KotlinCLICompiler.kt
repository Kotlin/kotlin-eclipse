package org.jetbrains.kotlin.core.launch

import java.io.PrintStream
import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.compiler.CompileEnvironmentException
import org.jetbrains.kotlin.config.Services
import kotlin.jvm.JvmStatic
import org.jetbrains.kotlin.core.model.KOTLIN_COMPILER_PATH

object KotlinCLICompiler {
	private val obligatoryCompilerFlags
        get() = arrayOf(
            "-Xintellij-plugin-root=" + KOTLIN_COMPILER_PATH,
		    "-Xdisable-default-scripting-plugin"
		)
	
	@JvmStatic
	fun doMain(compiler: CLICompiler<*>, errorStream: PrintStream, args: Array<String>): ExitCode {
		System.setProperty("java.awt.headless", "true")
		val exitCode = doMainNoExitWithHtmlOutput(compiler, errorStream, args)
		return exitCode
	}

	private fun doMainNoExitWithHtmlOutput(
			compiler: CLICompiler<*>,
			errorStream: PrintStream,
			args: Array<String>): ExitCode =
		try {
			compiler.execAndOutputXml(
					errorStream,
					Services.EMPTY,
					*obligatoryCompilerFlags,
					*args)
		} catch (e: CompileEnvironmentException) {
			errorStream.println(e.message)
			ExitCode.INTERNAL_ERROR
		}
	
}