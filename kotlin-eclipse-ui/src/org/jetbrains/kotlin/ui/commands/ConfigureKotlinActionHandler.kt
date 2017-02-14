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
package org.jetbrains.kotlin.ui.commands

import org.eclipse.core.commands.AbstractHandler
import org.eclipse.core.commands.ExecutionEvent
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jface.viewers.IStructuredSelection
import org.eclipse.ui.ISources
import org.jetbrains.kotlin.core.model.isConfigurationMissing
import org.eclipse.ui.handlers.HandlerUtil
import org.jetbrains.kotlin.wizards.NewUnitWizard
import org.jetbrains.kotlin.ui.launch.KotlinRuntimeConfigurator
import org.jetbrains.kotlin.core.model.KotlinNature

public class ConfigureKotlinActionHandler : AbstractHandler() {
    override fun execute(event: ExecutionEvent): Any? {
        val selection = HandlerUtil.getActiveMenuSelection(event)
        val project = getFirstOrNullJavaProject(selection as IStructuredSelection)!!.getProject()
        
        KotlinNature.addNature(project)
        KotlinRuntimeConfigurator.suggestForProject(project);
        
        return null
    }
    
    override fun setEnabled(evaluationContext: Any) {
        val selection = HandlerUtil.getVariable(evaluationContext, ISources.ACTIVE_CURRENT_SELECTION_NAME)
        if (selection is IStructuredSelection) {
            val javaProject = getFirstOrNullJavaProject(selection)
            if (javaProject != null) {
                setBaseEnabled(isConfigurationMissing(javaProject.getProject()))
                return
            }
        }
        
        setBaseEnabled(false)
    }
}