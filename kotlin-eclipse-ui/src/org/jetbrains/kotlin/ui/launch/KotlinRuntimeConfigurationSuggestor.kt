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
import org.eclipse.jface.dialogs.MessageDialogWithToggle
import org.eclipse.swt.widgets.Display
import org.jetbrains.kotlin.core.utils.ProjectUtils
import org.jetbrains.kotlin.eclipse.ui.utils.ProjectScopedPreferenceUtils

class KotlinRuntimeConfigurationSuggestor(private val project: IProject) : Runnable {
    companion object {
        private val SUGGESTION = "the Kotlin runtime library"
        private val MESSAGE_DIALOG_TITLE = "Add $SUGGESTION"
        private val MESSAGE_DIALOG_TEXT_FORMAT = "Would you like to add %s to the project \'%s\'?"
        private val MESSAGE_DIALOG_TOOGLE_TEXT = "Don't ask again for this project"
        private val PREFERENCE_KEY = "suggest.Configure.Runtime"

        @JvmStatic fun suggestForProject(project: IProject) {
            Display.getDefault().asyncExec(KotlinRuntimeConfigurationSuggestor(project))
        }
    }

    override fun run() {
        if (ProjectUtils.hasKotlinRuntime(project)) return

        if (ProjectScopedPreferenceUtils.getBooleanPreference(project, PREFERENCE_KEY, true)) {
            val dialogWithToogle = MessageDialogWithToggle.openYesNoQuestion(
                    Display.getDefault().getActiveShell(),
                    MESSAGE_DIALOG_TITLE,
                    String.format(MESSAGE_DIALOG_TEXT_FORMAT, SUGGESTION, project.getName()),
                    MESSAGE_DIALOG_TOOGLE_TEXT,
                    false,
                    null,
                    null)
            if (dialogWithToogle.returnCode == 2) {
                ProjectUtils.addKotlinRuntime(project)
            }

            if (dialogWithToogle.getToggleState()) {
                ProjectScopedPreferenceUtils.putBooleanPreference(project, PREFERENCE_KEY, false)
            }
        }
    }
}