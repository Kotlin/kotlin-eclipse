/*******************************************************************************
* Copyright 2000-2014 JetBrains s.r.o.
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

class KotlinLaunchShortcut : ILaunchShortcut {
    companion object {
        fun createConfiguration(file: IFile): ILaunchConfiguration {
            val configWC = getLaunchConfigurationType().newInstance(null, "Config - " + file.getName())
            configWC.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, getFileClassName(file).asString())
            configWC.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, file.getProject().getName())
            
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
        
        val fileWithMain = ProjectUtils.findFilesWithMain(files)
        if (fileWithMain != null) {
            launchWithMainClass(fileWithMain, mode)
            return
        }
        
        launchProject(files[0].getProject(), mode)
    }
    
    override fun launch(editor: IEditorPart, mode: String) {
        if (editor !is KotlinFileEditor) return
        
        val file = EditorUtil.getFile(editor)
        if (file == null) {
            KotlinLogger.logError("Failed to retrieve IFile from editor " + editor, null)
            return
        }
        
        if (ProjectUtils.hasMain(file)) {
            launchWithMainClass(file, mode)
            return
        }
        
        launchProject(file.getProject(), mode)
    }
    
    private fun launchProject(project: IProject, mode: String) {
        val fileWithMain = ProjectUtils.findFilesWithMain(KotlinPsiManager.INSTANCE.getFilesByProject(project))
        if (fileWithMain != null) {
            launchWithMainClass(fileWithMain, mode)
        }
    }
    
    private fun launchWithMainClass(fileWithMain: IFile, mode: String) {
        val configuration = findLaunchConfiguration(getLaunchConfigurationType(), fileWithMain) ?: createConfiguration(fileWithMain)
        DebugUITools.launch(configuration, mode)
    }
    
    private fun findLaunchConfiguration(configurationType: ILaunchConfigurationType, mainClass: IFile): ILaunchConfiguration? {
        val configs = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations(configurationType)
        val mainClassName = getFileClassName(mainClass).asString()
        for (config in configs) {
            if (config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, null as String?) == mainClassName && 
                config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, null as String?) == mainClass.project.name) {
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