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

import org.eclipse.core.resources.IResourceChangeEvent
import org.eclipse.core.resources.IResourceChangeListener
import org.eclipse.core.runtime.CoreException
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.eclipse.core.resources.IResourceDeltaVisitor
import org.eclipse.core.resources.IResourceDelta
import org.eclipse.core.resources.IFile
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.eclipse.core.resources.IProject

public class ResourceChangeListener : IResourceChangeListener {
	override public fun resourceChanged(event: IResourceChangeEvent) {
		if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
			event.getDelta().accept(ProjectChangeListener())
		}
	}
}

class ProjectChangeListener : IResourceDeltaVisitor {
	override public fun visit(delta: IResourceDelta) : Boolean {
		if (delta.getKind() == IResourceDelta.CHANGED) {
			return true
		}
		
        val resource = delta.getResource()
		when (resource) {
			is IFile -> {
				if (KotlinPsiManager.INSTANCE.isKotlinSourceFile(resource)) {
					KotlinPsiManager.INSTANCE.updateProjectPsiSources(resource, delta.getKind())
				}
			}
			is IProject -> KotlinPsiManager.INSTANCE.updateProjectPsiSources(resource, delta.getKind())
		}
		
		return true
	}
}