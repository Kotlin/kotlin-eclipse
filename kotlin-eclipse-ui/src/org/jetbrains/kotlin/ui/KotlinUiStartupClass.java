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
package org.jetbrains.kotlin.ui;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IStartup;

public class KotlinUiStartupClass implements IStartup {
    public static final String PRIVACY_POLICY_SHOWED_KEY = "kotlin.privacyPolicyCheck";
    
    @Override
    public void earlyStartup() {
        // activating Kotlin UI plugin on workbench startup
        
        IPreferenceStore preferenceStore = Activator.getDefault().getPreferenceStore();
        boolean privacyPolicyShowed = preferenceStore.getBoolean(PRIVACY_POLICY_SHOWED_KEY);
        if (!privacyPolicyShowed) {
            showPrivacyPolicy();
            preferenceStore.setValue(PRIVACY_POLICY_SHOWED_KEY, true);
        }
    }
    
    private void showPrivacyPolicy() {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                MessageDialog.openInformation(
                        Display.getDefault().getActiveShell(),
                        "Kotlin plugin usage",
                        "By clicking 'OK' you are agreeing with the JetBrains privacy policy (link)");
            }
        });
    }
}
