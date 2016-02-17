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
package org.jetbrains.kotlin.ui.launch

import java.util.ArrayList
import org.eclipse.core.resources.IContainer
import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.IResource
import org.eclipse.core.runtime.CoreException
import org.eclipse.core.runtime.IAdaptable
import org.eclipse.debug.core.DebugPlugin
import org.eclipse.debug.core.ILaunchConfiguration
import org.eclipse.debug.core.ILaunchConfigurationType
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy
import org.eclipse.debug.ui.DebugUITools
import org.eclipse.debug.ui.ILaunchShortcut
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants
import org.eclipse.jface.viewers.ISelection
import org.eclipse.jface.viewers.IStructuredSelection
import org.eclipse.ui.IEditorPart
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.core.utils.ProjectUtils
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import org.jetbrains.kotlin.fileClasses.getFileClassFqName
import org.jetbrains.kotlin.fileClasses.NoResolveFileClassesProvider
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.eclipse.jdt.core.JavaCore
import org.jetbrains.kotlin.psi.KtDeclaration

class KotlinLaunchShortcut : ILaunchShortcut {
    companion object {
        fun createConfiguration(entryPoint: KtDeclaration, project: IProject): ILaunchConfiguration? {
            val classFqName = getStartClassFqName(entryPoint)
            if (classFqName == null) return null
            
            val configWC = getLaunchConfigurationType().newInstance(null, "Config - " + entryPoint.getContainingKtFile().getName())
            configWC.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, classFqName.asString())
            configWC.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, project.getName())
            
            return configWC.doSave()
        }
        
        fun getFileClassName(mainFile: IFile): FqName {
            val ktFile = KotlinPsiManager.INSTANCE.getParsedFile(mainFile)
            return NoResolveFileClassesProvider.getFileClassFqName(ktFile)
        }
        
        private fun getLaunchConfigurationType(): ILaunchConfigurationType {
            return DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION)
        }
    }
    
    override fun launch(selection: ISelection, mode: String) {
        if (selection !is IStructuredSelection) return
        
        val files = ArrayList<IFile>()
        for (element in selection.toList()) {
            if (element is IAdaptable) {
                val resource = element.getAdapter(IResource::class.java)
                addFiles(files, resource)
            }
        }
        
        if (files.isEmpty()) return
        
        val mainFile = files.first()
        val javaProject = JavaCore.create(mainFile.getProject())
        val ktFile = KotlinPsiManager.INSTANCE.getParsedFile(mainFile)
        val entryPoint = getEntryPoint(ktFile, javaProject)
        if (entryPoint != null) {
            launchWithMainClass(entryPoint, javaProject.project, mode)
        }
    }
    
    override fun launch(editor: IEditorPart, mode: String) {
        if (editor !is KotlinFileEditor) return
        
        val file = editor.getFile()
        if (file == null) {
            KotlinLogger.logError("Failed to retrieve IFile from editor " + editor, null)
            return
        }
        
        val parsedFile = editor.parsedFile
        if (parsedFile == null) return
        
        val javaProject = editor.javaProject
        if (javaProject == null) return
        
        val entryPoint = getEntryPoint(parsedFile, javaProject)
        if (entryPoint != null) {
            launchWithMainClass(entryPoint, javaProject.project, mode)
            return
        }
    }
    
    private fun launchWithMainClass(entryPoint: KtDeclaration, project: IProject, mode: String) {
        val configuration = findLaunchConfiguration(getLaunchConfigurationType(), entryPoint, project) ?: 
                createConfiguration(entryPoint, project)
        
        if (configuration != null) {
            DebugUITools.launch(configuration, mode)
        }
    }
    
    private fun findLaunchConfiguration(
            configurationType: ILaunchConfigurationType, 
            entryPoint: KtDeclaration,
            project: IProject): ILaunchConfiguration? {
        val configs = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations(configurationType)
        val mainClassName = getStartClassFqName(entryPoint)?.asString()
        if (mainClassName == null) return null
        
        for (config in configs) {
            if (config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, null as String?) == mainClassName && 
                config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, null as String?) == project.name) {
                return config
            }
        }
        
        return null
    }
    
    private fun addFiles(files: ArrayList<IFile>, resource: IResource) {
        when (resource.getType()) {
            IResource.FILE -> {
                val file = resource as IFile
                if (resource.getFileExtension() == KotlinFileType.INSTANCE.getDefaultExtension()) {
                    files.add(file)
                }
            }
            
            IResource.FOLDER, IResource.PROJECT -> {
                val container = resource as IContainer
                for (child in container.members()) {
                    addFiles(files, child)
                }
            }
        }
    }
}