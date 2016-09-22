/*******************************************************************************
* Copyright 2000-2016 JetBrains s.r.o.
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
package org.jetbrains.kotlin.core.model

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.impl.ZipHandler
import java.util.HashMap

class CachedEnvironment<T, E : KotlinCommonEnvironment> {
    private val environmentLock = Any()
    
    private val environmentCache: HashMap<T, E> = hashMapOf()
    private val ideaProjectToEclipseResource: HashMap<Project, T> = hashMapOf()
    
    fun putEnvironment(resource: T, environment: E) {
        synchronized(environmentLock) {
            environmentCache.put(resource, environment)
            ideaProjectToEclipseResource.put(environment.project, resource)
        }
    }
    
    fun getOrCreateEnvironment(resource: T, createEnvironment: (T) -> E): E {
        return synchronized(environmentLock) {
                environmentCache.getOrPut(resource) {
                val newEnvironment = createEnvironment(resource)
                ideaProjectToEclipseResource.put(newEnvironment.project, resource)
                
                newEnvironment
            }
        }
    }
    
    fun updateEnvironment(resource: T, createEnvironment: (T) -> E) {
        synchronized (environmentLock) {
            if (environmentCache.containsKey(resource)) {
                removeEnvironment(resource)
            }
            
            val newEnvironment = createEnvironment(resource)
            environmentCache.put(resource, newEnvironment)
            ideaProjectToEclipseResource.put(newEnvironment.project, resource)
        }
    }
    
    fun removeEnvironment(resource: T) {
        synchronized (environmentLock) {
            if (environmentCache.containsKey(resource)) {
                val environment = environmentCache[resource]!!
                
                ideaProjectToEclipseResource.remove(environment.project)
                environmentCache.remove(resource)
                
                Disposer.dispose(environment.javaApplicationEnvironment.getParentDisposable())
                ZipHandler.clearFileAccessorCache()
            }
        }
    }
    
    fun getEclipseResource(ideaProject: Project): T? {
        return synchronized(environmentLock) {
            ideaProjectToEclipseResource.get(ideaProject)
        }
    }
}