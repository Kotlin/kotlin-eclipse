package org.jetbrains.kotlin.ui

import com.intellij.openapi.components.ServiceManager
import org.eclipse.core.resources.*
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.Status
import org.eclipse.core.runtime.jobs.Job
import org.eclipse.jdt.core.JavaCore
import org.eclipse.ui.PlatformUI
import org.jetbrains.kotlin.core.model.*
import org.jetbrains.kotlin.core.utils.ProjectUtils
import org.jetbrains.kotlin.core.utils.asFile
import org.jetbrains.kotlin.scripting.resolve.ScriptContentLoader
import org.jetbrains.kotlin.ui.editors.KotlinScriptEditor
import kotlin.script.dependencies.Environment
import kotlin.script.experimental.dependencies.DependenciesResolver
import kotlin.script.experimental.dependencies.ScriptDependencies


class ScriptClasspathUpdater : IResourceChangeListener {
    override fun resourceChanged(event: IResourceChangeEvent) {
        val delta = event.delta ?: return
        delta.accept(object : IResourceDeltaVisitor {
            override fun visit(delta: IResourceDelta?): Boolean {
                val resource = delta?.resource ?: return false
                if (resource is IFile) {
                    tryUpdateScriptClasspath(resource)
                    return false
                }

                return true
            }
        })
    }
}

internal fun tryUpdateScriptClasspath(file: IFile) {
    if (findEditor(file) == null) return

    val environment = getEnvironment(file) as? KotlinScriptEnvironment ?: return

    val dependenciesProvider = ServiceManager.getService(environment.project, DependenciesResolver::class.java)

    runJob("Check script dependencies", Job.DECORATE, null, {
        val contents = ScriptContentLoader(environment.project).getScriptContents(
            environment.definition?.legacyDefinition!!,
            environment.getVirtualFile(file.location)!!
        )

        val scriptEnvironment = EclipseScriptDefinitionProvider.getEnvironment(file.asFile)
        val newDependencies = dependenciesProvider.resolve(contents, scriptEnvironment)
        StatusWithDependencies(Status.OK_STATUS, newDependencies.dependencies)
    }) { event ->
        val editor = findEditor(file)
        val statusWithDependencies = event.result
        val newDependencies = (statusWithDependencies as? StatusWithDependencies)?.dependencies
        if (file.isAccessible && editor != null) {
            editor.reconcile {
                KotlinScriptEnvironment.updateDependencies(file, newDependencies)
                KotlinAnalysisFileCache.resetCache()
            }
        }
    }
}

private data class StatusWithDependencies(val status: IStatus, val dependencies: ScriptDependencies?) :
    IStatus by status

private fun findEditor(scriptFile: IFile): KotlinScriptEditor? {
    for (window in PlatformUI.getWorkbench().getWorkbenchWindows()) {
        for (page in window.getPages()) {
            for (editorReference in page.getEditorReferences()) {
                val editor = editorReference.getEditor(false)
                if (editor !is KotlinScriptEditor) continue

                if (editor.eclipseFile == scriptFile) return editor
            }
        }
    }

    return null
}