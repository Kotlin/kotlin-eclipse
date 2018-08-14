package org.jetbrains.kotlin.core.script.template

import java.io.File
import kotlin.script.dependencies.Environment
import kotlin.script.dependencies.ScriptContents
import kotlin.script.experimental.dependencies.DependenciesResolver
import kotlin.script.experimental.dependencies.ScriptDependencies
import kotlin.script.experimental.dependencies.asSuccess

class ProjectFilesResolver : DependenciesResolver {
    override fun resolve(scriptContents: ScriptContents, environment: Environment): DependenciesResolver.ResolveResult {
        val classpath = (environment["eclipseProjectClasspath"] as? String)
            ?.split(":")
            ?.map { File(it) }
            .orEmpty()

        return ScriptDependencies(classpath = classpath).asSuccess()
    }
}