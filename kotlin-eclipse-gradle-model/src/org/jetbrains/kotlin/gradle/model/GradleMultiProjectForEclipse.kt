package org.jetbrains.kotlin.gradle.model

import java.io.Serializable

interface GradleMultiProjectForEclipse : Serializable {
    operator fun get(name: String): GradleProjectForEclipse?
}

class GradleMultiProjectForEclipseImpl(
    private val subprojects: Map<String, GradleProjectForEclipse>
) : GradleMultiProjectForEclipse {
    override fun get(name: String) = subprojects[name]
}