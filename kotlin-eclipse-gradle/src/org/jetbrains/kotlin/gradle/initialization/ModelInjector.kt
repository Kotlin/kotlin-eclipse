package org.jetbrains.kotlin.gradle.initialization

import org.eclipse.buildship.core.invocation.InvocationCustomizer
import org.eclipse.core.runtime.Platform
import org.jetbrains.kotlin.gradle.Activator
import org.eclipse.core.runtime.FileLocator
import java.io.File
import java.lang.management.ManagementFactory
import org.jetbrains.kotlin.core.log.KotlinLogger

class ModelInjector : InvocationCustomizer {
    override fun getExtraArguments() = listOf("--init-script", initializerPath)
        .also { System.setProperty(MODEL_PATH_PROPERTY, modelLibPath) }

    companion object {
        private const val MODEL_PATH_PROPERTY = "org.jetbrains.kotlin.eclipse.gradle.model.path"

        private val initializerPath by lazy {
            Platform.getBundle(Activator.PLUGIN_ID)
                .getEntry("scripts/init.gradle.kts")
                .let(FileLocator::toFileURL)
                .path
        }

        private val modelLibPath by lazy<String> {
            Platform.getBundle("org.jetbrains.kotlin.gradle.model")
                .getEntry("lib/kotlin-eclipse-gradle-model.jar")
                .let(FileLocator::toFileURL)
                .path
        }
    }
}