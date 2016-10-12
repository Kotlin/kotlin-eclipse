package org.jetbrains.kotlin.core.model

import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.IResource
import org.eclipse.core.resources.IResourceChangeEvent
import org.eclipse.core.resources.IResourceChangeListener
import org.jetbrains.kotlin.core.asJava.KotlinLightClassGeneration

public object KotlinRefreshProjectListener : IResourceChangeListener {
    override fun resourceChanged(event: IResourceChangeEvent) {
        if (event.getType() == IResourceChangeEvent.PRE_REFRESH) {
            val delta = event.getDelta()
            if (delta == null) {
                tryUpdateLightClassesFor(event.resource)
                return
            }
            
            delta.accept { visitorDelta ->
                val resource = visitorDelta.getResource()
                if (resource is IProject) {
                    tryUpdateLightClassesFor(resource)
                    return@accept false
                }
                
                true
            }
        }
    }
    
    private fun tryUpdateLightClassesFor(resource: IResource?) {
        if (resource is IProject && KotlinNature.hasKotlinNature(resource)) {
            KotlinLightClassGeneration.updateLightClasses(resource, emptySet())
        }
    }
}