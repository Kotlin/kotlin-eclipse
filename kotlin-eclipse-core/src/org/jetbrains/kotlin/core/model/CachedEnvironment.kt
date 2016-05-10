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

import org.eclipse.jdt.core.IJavaProject
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.core.model.CachedEnvironment
import com.intellij.openapi.util.Disposer
import java.util.HashMap
import com.intellij.openapi.vfs.impl.ZipHandler

class CachedEnvironment {
    private val environmentLock = Any()
    
    private val environmentCache: HashMap<IJavaProject, KotlinEnvironment> = hashMapOf()
    private val ideaToEclipseProject: HashMap<Project, IJavaProject> = hashMapOf()
    
    fun putEnvironment(javaProject: IJavaProject, environment: KotlinEnvironment) {
        synchronized(environmentLock) {
            environmentCache.put(javaProject, environment)
            ideaToEclipseProject.put(environment.project, javaProject)
        }
    }
    
    fun getOrCreateEnvironment(javaProject: IJavaProject, createEnvironment: (IJavaProject) -> KotlinEnvironment): KotlinEnvironment {
        return synchronized(environmentLock) {
                environmentCache.getOrPut(javaProject) {
                val newEnvironment = createEnvironment(javaProject)
                ideaToEclipseProject.put(newEnvironment.project, javaProject)
                
                newEnvironment
            }
        }
    }
    
    fun updateEnvironment(javaProject: IJavaProject, createEnvironment: (IJavaProject) -> KotlinEnvironment) {
        synchronized (environmentLock) {
            if (environmentCache.containsKey(javaProject)) {
                val environment = environmentCache.get(javaProject)!!
                
                ideaToEclipseProject.remove(environment.getProject())
                
                Disposer.dispose(environment.getJavaApplicationEnvironment().getParentDisposable())
                ZipHandler.clearFileAccessorCache()
            }
            
            val newEnvironment = createEnvironment(javaProject)
            environmentCache.put(javaProject, newEnvironment);
            ideaToEclipseProject.put(newEnvironment.getProject(), javaProject);
        }
    }
    
    fun getJavaProject(ideaProject: Project): IJavaProject? {
        return synchronized(environmentLock) {
            ideaToEclipseProject.get(ideaProject)
        }
    }
}