package org.jetbrains.kotlin.core

import org.eclipse.debug.core.ILaunchConfiguration
import org.eclipse.jdt.launching.IRuntimeClasspathEntry
import org.eclipse.jdt.launching.JavaRuntime
import org.eclipse.jdt.launching.StandardClasspathProvider

const val KOTLIN_CLASSPATH_PROVIDER_ID = "org.jetbrains.kotlin.core.kotlinClasspathProvider"

class KotlinClasspathProvider : StandardClasspathProvider() {
    override fun computeUnresolvedClasspath(configuration: ILaunchConfiguration): Array<IRuntimeClasspathEntry> =
            super.computeUnresolvedClasspath(configuration)
                    .filterNot { it.location != null && it.location == configuration.lightClassPath }
                    .toTypedArray()

    private val ILaunchConfiguration.lightClassPath: String?
        get() = JavaRuntime.getJavaProject(this)
                ?.let { KotlinClasspathContainer.getPathToLightClassesFolder(it) }
                ?.toPortableString()
}