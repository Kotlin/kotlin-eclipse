package org.jetbrains.kotlin.ui

import org.eclipse.core.resources.IResourceChangeListener
import org.eclipse.core.resources.IResourceChangeEvent
import org.eclipse.core.resources.IResourceDeltaVisitor
import org.eclipse.core.resources.IResourceDelta
import org.eclipse.core.resources.IFile
import org.jetbrains.kotlin.core.model.getEnvironment
import org.jetbrains.kotlin.core.model.KotlinScriptEnvironment

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
    val environment = getEnvironment(file)
    if (environment !is KotlinScriptEnvironment) return
    
    environment.initializeScriptDefinitions { scriptDefinitions, classpath ->
        if (file.isAccessible) {
            KotlinScriptEnvironment.replaceEnvironment(file, scriptDefinitions, classpath)
        }
    }
}