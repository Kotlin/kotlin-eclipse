package org.jetbrains.kotlin.ui

import org.eclipse.core.resources.IResourceChangeListener
import org.eclipse.core.resources.IResourceChangeEvent
import org.eclipse.core.resources.IResourceDeltaVisitor
import org.eclipse.core.resources.IResourceDelta
import org.eclipse.core.resources.IFile
import org.jetbrains.kotlin.core.model.getEnvironment
import org.jetbrains.kotlin.core.model.KotlinScriptEnvironment
import org.eclipse.ui.PlatformUI
import org.jetbrains.kotlin.ui.editors.KotlinScriptEditor
import org.eclipse.ui.IEditorPart
import org.jetbrains.kotlin.core.model.runJob
import org.eclipse.core.runtime.Status
import org.eclipse.core.runtime.jobs.Job
import org.jetbrains.kotlin.core.model.KotlinAnalysisFileCache
import org.jetbrains.kotlin.script.KotlinScriptDefinitionProvider
import org.jetbrains.kotlin.script.KotlinScriptExternalDependencies
import org.eclipse.core.runtime.IStatus
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.script.ScriptDependenciesProvider
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import kotlin.script.experimental.dependencies.ScriptDependencies

class ScriptClasspathUpdater : IResourceChangeListener {
    override public fun resourceChanged(event: IResourceChangeEvent) {
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

private fun tryUpdateScriptClasspath(file: IFile) {
    if (findEditor(file) == null) return
    
    val environment = getEnvironment(file)
    if (environment !is KotlinScriptEnvironment) return
    if (environment.loadScriptDefinitions || environment.isInitializingScriptDefinitions) return
    
    val dependenciesProvider = ScriptDependenciesProvider.getInstance(environment.project)
    
    runJob("Check script dependencies", Job.DECORATE, null, {
    	val newDependencies = dependenciesProvider.getScriptDependencies(KotlinPsiManager.getParsedFile(file))
//        KotlinLogger.logInfo("Check for script definition: ${dependenciesProvider}")
//        KotlinLogger.logInfo("New dependencies: ${newDependencies?.classpath?.joinToString("\n") { it.absolutePath }}")
        StatusWithDependencies(Status.OK_STATUS, newDependencies)
    }) { event ->
        val editor = findEditor(file)
        val statusWithDependencies = event.result
        val newDependencies = (statusWithDependencies as? StatusWithDependencies)?.dependencies
        if (file.isAccessible && editor != null) {
//            KotlinLogger.logInfo("Set new dependencies!!")
        	editor.reconcile {
        		KotlinScriptEnvironment.replaceEnvironment(file, environment.scriptDefinitions, environment.providersClasspath, newDependencies)
        		KotlinAnalysisFileCache.resetCache()
            }
        }
        else {
//            KotlinLogger.logInfo("Don't set new dependencies: accessible (${file.isAccessible}), editor (${editor})")
        }
    }
}

private data class StatusWithDependencies(val status: IStatus, val dependencies: ScriptDependencies?): IStatus by status

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