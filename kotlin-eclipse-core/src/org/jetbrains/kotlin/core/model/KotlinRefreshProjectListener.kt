package org.jetbrains.kotlin.core.model

import org.eclipse.core.resources.IResourceChangeListener
import org.eclipse.core.resources.IResourceChangeEvent
import org.eclipse.core.resources.IProject
import org.eclipse.jdt.core.JavaCore
import org.jetbrains.kotlin.core.asJava.KotlinLightClassGeneration

public object KotlinRefreshProjectListener : IResourceChangeListener {
    override fun resourceChanged(event: IResourceChangeEvent) {
        if (event.getType() == IResourceChangeEvent.PRE_REFRESH) {
            event.getDelta()?.accept { delta ->
                val resource = delta.getResource()
                if (resource != null && resource is IProject) {
                    val javaProject = JavaCore.create(resource)
                    KotlinLightClassGeneration.updateLightClasses(javaProject, emptySet())
                    
                    return@accept false
                }
                
                true
            }
        }
    }
}