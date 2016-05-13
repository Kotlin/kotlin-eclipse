/*******************************************************************************
* Copyright 2000-2015 JetBrains s.r.o.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*
*******************************************************************************/
package org.jetbrains.kotlin.ui.builder

import org.eclipse.core.resources.*
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.core.model.KotlinNature
import org.jetbrains.kotlin.core.model.setKotlinBuilderBeforeJavaBuilder

public class ResourceChangeListener : IResourceChangeListener {
    override public fun resourceChanged(event: IResourceChangeEvent) {
        val eventResource = event.resource
        if (eventResource != null) {
            val type = event.type
            if (type == IResourceChangeEvent.PRE_CLOSE || type == IResourceChangeEvent.PRE_DELETE) {
                updateManager(eventResource, IResourceDelta.REMOVED)
            }
            
            return
        }
        
        val delta = event.delta
        if (delta != null) {
            delta.accept(ProjectChangeListener())
        }
    }
}

class ProjectChangeListener : IResourceDeltaVisitor {
    override public fun visit(delta: IResourceDelta) : Boolean {
        val resource = delta.resource
        if ((delta.flags and IResourceDelta.OPEN) != 0) {
            if (resource is IProject && resource.isOpen) {
                return updateManager(resource, IResourceDelta.ADDED)
            }
        }
        
        if (delta.getKind() == IResourceDelta.CHANGED) {
            return true
        }
        
        return updateManager(resource, delta.kind)
    }
}

private fun updateManager(resource: IResource, deltaKind: Int): Boolean {
    return when (resource) {
        is IFile -> {
            if (KotlinPsiManager.isKotlinSourceFile(resource)) {
                KotlinPsiManager.INSTANCE.updateProjectPsiSources(resource, deltaKind)
            }
            
            false
        }
        
        is IProject -> {
            if (!resource.isAccessible || !KotlinNature.hasKotlinNature(resource)) {
                return false
            }
            
            if (deltaKind == IResourceDelta.REMOVED) {
                KotlinPsiManager.INSTANCE.removeProjectFromManager(resource)
            }
            
            if (deltaKind == IResourceDelta.ADDED && KotlinNature.hasKotlinBuilder(resource)) {
                setKotlinBuilderBeforeJavaBuilder(resource)
            }
            
            false
        }
        
        else -> true // folder
    }
}