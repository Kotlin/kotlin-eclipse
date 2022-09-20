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
import org.eclipse.jdt.core.JavaCore
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.core.model.*
import org.jetbrains.kotlin.core.utils.ProjectUtils
import org.jetbrains.kotlin.core.utils.asFile
import kotlin.script.experimental.host.FileScriptSource

class ResourceChangeListener : IResourceChangeListener {
    override fun resourceChanged(event: IResourceChangeEvent) {
        val eventResource = event.resource
        if (eventResource != null) {
            val type = event.type
            if (type == IResourceChangeEvent.PRE_CLOSE || type == IResourceChangeEvent.PRE_DELETE) {
                updateManager(eventResource, IResourceDelta.REMOVED)
            }

            return
        }

        event.delta?.accept(ProjectChangeListener())
    }
}

class ProjectChangeListener : IResourceDeltaVisitor {
    override fun visit(delta: IResourceDelta): Boolean {
        val resource = delta.resource
        if ((delta.flags and IResourceDelta.OPEN) != 0) {
            if (resource is IProject && resource.isOpen) {
                return updateManager(resource, IResourceDelta.ADDED)
            }
        }

        if (delta.kind == IResourceDelta.CHANGED) {
            return true
        }

        return updateManager(resource, delta.kind)
    }
}

private fun updateManager(resource: IResource, deltaKind: Int): Boolean {
    return when (resource) {
        is IFile -> {
            //IF we got a source file we update the psi!
            if (KotlinPsiManager.isKotlinSourceFile(resource)) {
                KotlinPsiManager.updateProjectPsiSources(resource, deltaKind)
            }

            //If we got a script file and it was deleted we remove the environment.
            if(EclipseScriptDefinitionProvider.isScript(FileScriptSource(resource.asFile))) {
                if(deltaKind == IResourceDelta.REMOVED) {
                    KotlinScriptEnvironment.removeKotlinEnvironment(resource)
                }
            }
            false
        }

        is IProject -> {
            //If we got a project removed we need to remove all environments that represent
            //this project or dependet on that project. For simplicity we remove all environments for now.
            if (deltaKind == IResourceDelta.REMOVED) {
                KotlinPsiManager.removeProjectFromManager(resource)
                KotlinEnvironment.removeEnvironmentIf { it: KotlinCommonEnvironment ->
                    val tempDepPrjs = ProjectUtils.getDependencyProjects(it.javaProject)
                    resource in tempDepPrjs
                }
                KotlinScriptEnvironment.removeEnvironmentIf { it: KotlinCommonEnvironment ->
                    val tempDepPrjs = ProjectUtils.getDependencyProjects(it.javaProject)
                    resource in tempDepPrjs
                }
            }

            //If a project was added we need to make sure the kotlin builder is invoked before the java builder.
            //Also we need to refresh all environments that had errors before, as they could be resolved by the new project.
            if (deltaKind == IResourceDelta.ADDED) {
                if(KotlinNature.hasKotlinBuilder(resource)) {
                    setKotlinBuilderBeforeJavaBuilder(resource)
                }
                KotlinEnvironment.removeEnvironmentIf { it.hasError }
                KotlinScriptEnvironment.removeEnvironmentIf { it.hasError }
            }

            false
        }

        else -> true // folder
    }
}