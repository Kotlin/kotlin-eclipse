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

import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.IResource
import org.eclipse.core.runtime.IAdaptable
import org.eclipse.debug.core.DebugPlugin
import org.eclipse.debug.core.ILaunchConfiguration
import org.eclipse.debug.core.ILaunchConfigurationType
import org.eclipse.debug.ui.DebugUITools
import org.eclipse.debug.ui.ILaunchShortcut
import org.eclipse.jface.viewers.ISelection
import org.eclipse.jface.viewers.IStructuredSelection
import org.eclipse.ui.IEditorPart
import org.jetbrains.kotlin.ui.editors.KotlinScriptEditor
import java.util.ArrayList

const val SCRIPT_FILE_PATH = "org.jetbrains.kotlin.ui.scrpitFilePathAttribute"
private const val SCRIPT_LAUNCH_CONFIGURATION_TYPE = "org.jetbrains.kotlin.ui.scriptLaunchConfiguration"

class KotlinScriptLaunchShortcut : ILaunchShortcut {
    override fun launch(selection: ISelection, mode: String) {
        val scriptFile = findFileToLaunch(selection) ?: return
        launch(scriptFile, mode)
    }

    override fun launch(editor: IEditorPart, mode: String) {
        if (editor !is KotlinScriptEditor) return
        
        val scriptFile = editor.eclipseFile ?: return
        
        launch(scriptFile, mode)
    }
    
    private fun launch(scriptFile: IFile, mode: String) {
        val configuration = getLaunchConfigurationType().let {
            findLaunchConfiguration(scriptFile, it) ?: createLaunchConfiguration(scriptFile, it)
        }
        
        DebugUITools.launch(configuration, mode)
    }
    
    private fun findLaunchConfiguration(scriptFile: IFile, configurationType: ILaunchConfigurationType): ILaunchConfiguration? {
        return DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations(configurationType).find {
            it.getAttribute(SCRIPT_FILE_PATH, "") == scriptFile.fullPath.toPortableString()
        }
    }
    
    private fun createLaunchConfiguration(scriptFile: IFile,
            configurationType: ILaunchConfigurationType): ILaunchConfiguration {
        
        return configurationType.newInstance(null,  "Script - ${scriptFile.name}").apply {
            setAttribute(SCRIPT_FILE_PATH, scriptFile.fullPath.toPortableString())
        }.doSave()
    }
}

fun findFileToLaunch(selection: ISelection): IFile? {
    if (selection !is IStructuredSelection) return null
    
    return selection.toList()
            .filterIsInstance(IAdaptable::class.java)
            .mapNotNull { it.getAdapter(IFile::class.java) }
            .firstOrNull()

}

private fun getLaunchConfigurationType(): ILaunchConfigurationType {
    return DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationType(SCRIPT_LAUNCH_CONFIGURATION_TYPE)
}