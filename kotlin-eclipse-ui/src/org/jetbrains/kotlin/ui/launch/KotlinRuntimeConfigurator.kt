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

import org.eclipse.core.resources.IProject
import org.eclipse.jface.dialogs.MessageDialog
import org.eclipse.jface.window.Window
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.StyleRange
import org.eclipse.swt.custom.StyledText
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Shell
import org.eclipse.ui.ISharedImages
import org.eclipse.ui.PlatformUI
import org.jetbrains.kotlin.core.KotlinClasspathContainer
import org.jetbrains.kotlin.core.utils.ProjectUtils
import org.jetbrains.kotlin.ui.gridData

class KotlinRuntimeConfigurator(private val project: IProject) : Runnable {
    companion object {
        @JvmStatic fun suggestForProject(project: IProject) {
            Display.getDefault().asyncExec(KotlinRuntimeConfigurator(project))
        }
    }

    override fun run() {
        if (ProjectUtils.hasKotlinRuntime(project)) return

        ProjectUtils.addKotlinRuntime(project)
        
        MessageDialog.openInformation(Shell(Display.getDefault()), "Configure Kotlin in Project",
            KotlinClasspathContainer.LIB_RUNTIME_NAME + ".jar, " + KotlinClasspathContainer.LIB_REFLECT_NAME + ".jar were added\nto the project classpath.")
    }
}