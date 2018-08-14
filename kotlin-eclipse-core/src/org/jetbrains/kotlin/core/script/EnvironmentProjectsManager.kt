package org.jetbrains.kotlin.core.script

import org.eclipse.core.internal.resources.ProjectDescription
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.JavaCore
import org.jetbrains.kotlin.core.model.KotlinNature
import org.jetbrains.kotlin.core.utils.withResourceLock
import org.jetbrains.kotlin.psi.KtFile
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

object EnvironmentProjectsManager {
    private val projectCache: ConcurrentMap<String, IJavaProject> = ConcurrentHashMap()

    private val createdProjects = mutableSetOf<String>()

    operator fun get(scriptFile: KtFile) = projectCache.getOrPut(scriptFile.virtualFilePath) {
        withResourceLock(ResourcesPlugin.getWorkspace().root) { root ->
            nameCandidates(scriptFile).map { root.getProject(it) }
                .first { !it.exists() }
                .run {
                    createdProjects += name

                    val description = ProjectDescription().apply {
                        name = this@run.name
                        locationURI = URI.create("org.jetbrains.kotlin.script:/environments/${this@run.name}")
                        natureIds = arrayOf(JavaCore.NATURE_ID)
                    }

                    create(description, null)
                    open(null)
                    KotlinNature.addNature(this)
                    isHidden = true
                    JavaCore.create(this)
                }
        }
    }

    fun wasCreated(name: String) = name in createdProjects

    private fun nameCandidates(scriptFile: KtFile): Sequence<String> = generateSequence(1) { it + 1 }
        .map { n ->
            listOfNotNull(
                scriptFile.packageFqName.takeUnless { it.isRoot },
                scriptFile.name,
                n.takeIf { it > 1 }?.toString()
            ).joinToString("-")
        }
}