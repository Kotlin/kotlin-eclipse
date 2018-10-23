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
import org.eclipse.ui.handlers.HandlerUtil
import org.jetbrains.kotlin.core.utils.ProjectUtils
import org.jetbrains.kotlin.core.utils.buildLibPath

public class AddCoroutinesActionHandler : AbstractHandler() {
    override fun execute(event: ExecutionEvent): Any? {
        val selection = HandlerUtil.getActiveMenuSelection(event)
        val project = getFirstOrNullJavaProject(selection as IStructuredSelection)!!

        ProjectUtils.addToClasspath(
            project,
            ProjectUtils.newExportedLibraryEntry("kotlinx-coroutines-core".buildLibPath()),
            ProjectUtils.newExportedLibraryEntry("kotlinx-coroutines-jdk8".buildLibPath())
        )

        return null
    }

    override fun setEnabled(evaluationContext: Any) {
        val selection = HandlerUtil.getVariable(evaluationContext, ISources.ACTIVE_CURRENT_SELECTION_NAME)
        val newEnabled = (selection as? IStructuredSelection)
            ?.let { getFirstOrNullJavaProject(it) }
            ?.let { needsCoroutinesLibrary(it) }
            ?: false

        setBaseEnabled(newEnabled)
    }

    private fun needsCoroutinesLibrary(javaProject: IJavaProject): Boolean =
        javaProject.findType("kotlinx.coroutines.CoroutineScope") == null
                && !ProjectUtils.isGradleProject(javaProject.project)
                && !ProjectUtils.isMavenProject(javaProject.project)
}