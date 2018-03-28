package org.jetbrains.kotlin.core.compiler

import java.net.URLClassLoader
import org.jetbrains.kotlin.core.model.KotlinEnvironment
import org.jetbrains.kotlin.core.utils.ProjectUtils
import java.net.URL
import java.io.PrintStream
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.io.File
import org.jetbrains.kotlin.core.model.KOTLIN_COMPILER_PATH
import java.lang.reflect.Proxy

object UserBundleLoader {
	private val classLoader: ClassLoader by lazy {
		ProjectUtils.buildLibPath("kotlin-compiler")
				.let { URL("file://$it") }
				.let { URLClassLoader(arrayOf(it), null) }
	}

	internal val k2JVMCompiler by lazilyLoaded("org.jetbrains.kotlin.cli.jvm.K2JVMCompiler")

	internal val cLICompiler by lazilyLoaded("org.jetbrains.kotlin.cli.common.CLICompiler")

	internal val servicesBuilder by lazilyLoaded("org.jetbrains.kotlin.config.Services\$Builder")

	internal val compilerJarLocator by lazilyLoaded("org.jetbrains.kotlin.cli.jvm.compiler.CompilerJarLocator")

	fun invokeCompiler(output: PrintStream, arguments: Array<String>) {
		val services = servicesBuilder.newInstance().apply {
			servicesBuilder.getMethod("register", Class::class.java, Any::class.java)
					.invoke(this, compilerJarLocator, Proxy.newProxyInstance(classLoader, arrayOf(compilerJarLocator), JarLocatorProxy))
		}.run {
			servicesBuilder.getMethod("build").invoke(this)
		}

		k2JVMCompiler.newInstance().run {
			cLICompiler.getMethod("execAndOutputXml", PrintStream::class.java, services::class.java, Array<String>::class.java)
					.invoke(this, output, services, arguments)
		}
	}

	private fun lazilyLoaded(name: String): Lazy<Class<*>> = lazy {
		Class.forName(name, true, classLoader)
	}

	private object JarLocatorProxy : InvocationHandler {
		override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any? {
			return File(KOTLIN_COMPILER_PATH)
		}
	}
}