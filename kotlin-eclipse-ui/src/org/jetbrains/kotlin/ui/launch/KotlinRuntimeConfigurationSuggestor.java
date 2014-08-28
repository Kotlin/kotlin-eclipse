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
package org.jetbrains.kotlin.ui.launch;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.swt.widgets.Display;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.utils.ProjectUtils;
import org.jetbrains.kotlin.utils.ProjectScopedPreferenceUtils;
import org.osgi.service.prefs.BackingStoreException;

public class KotlinRuntimeConfigurationSuggestor implements Runnable {
    
    private static final String SUGGESTION = "the Kotlin runtime library";
    private static final String MESSAGE_DIALOG_TITLE = String.format("Add %s", SUGGESTION);
    private static final String MESSAGE_DIALOG_TEXT_FORMAT = "Would you like to add %s to the project \'%s\'?";
    
    private static final String MESSAGE_DIALOG_TOOGLE_TEXT = "Don't ask again for this project";
    
    private static final String PREFERENCE_KEY = "suggest.Configure.Runtime";
    
    @NotNull
    private final IProject project;
    
    protected KotlinRuntimeConfigurationSuggestor(@NotNull IProject project) {
        this.project = project;
    }
    
    @Override
    public final void run() {
        try {
            if (!ProjectUtils.hasKotlinRuntime(project)) {
                if (ProjectScopedPreferenceUtils.getBooleanPreference(project, PREFERENCE_KEY, true)) {
                    MessageDialogWithToggle dialogWithToogle = MessageDialogWithToggle.openYesNoQuestion(
                            Display.getDefault().getActiveShell(),
                            MESSAGE_DIALOG_TITLE,
                            String.format(MESSAGE_DIALOG_TEXT_FORMAT, SUGGESTION, project.getName()),
                            MESSAGE_DIALOG_TOOGLE_TEXT,
                            false,
                            null,
                            null);
                    
                    if (dialogWithToogle.getReturnCode() == 2) {
                        ProjectUtils.addKotlinRuntime(project);
                    }
                    if (dialogWithToogle.getToggleState()) {
                        ProjectScopedPreferenceUtils.putBooleanPreference(project, PREFERENCE_KEY, false);
                    }
                }
            }
        } catch (CoreException | BackingStoreException e) {
            KotlinLogger.logAndThrow(e);
        }
    }
    
    public static void suggestForProject(@NotNull IProject project) {
        Display.getDefault().asyncExec(new KotlinRuntimeConfigurationSuggestor(project));
    }
}
