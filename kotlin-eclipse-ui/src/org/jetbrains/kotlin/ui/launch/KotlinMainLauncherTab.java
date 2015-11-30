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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaMainTab;
import org.eclipse.jdt.internal.ui.viewsupport.JavaUILabelProvider;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.dialogs.ListDialog;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.utils.ProjectUtils;

public class KotlinMainLauncherTab extends JavaMainTab implements ILaunchConfigurationTab {
    
    @Override
    public void handleSearchButtonSelected() {
        ListDialog dialog = new ListDialog(getShell());
        dialog.setBlockOnOpen(true);
        dialog.setMessage("Select a Kotlin file to run");
        dialog.setTitle("Choose Kotlin Compilation Unit");
        dialog.setContentProvider(new ArrayContentProvider());
        dialog.setLabelProvider(new JavaUILabelProvider());

        Collection<IFile> projectFiles = null;
        projectFiles = KotlinPsiManager.INSTANCE.getFilesByProject(fProjText.getText());
        
        List<IFile> mainFiles = new ArrayList<IFile>();
        for (IFile file : projectFiles) {
            if (ProjectUtils.hasMain(file)) {
                mainFiles.add(file);
            }
        }
        
        dialog.setInput(mainFiles);
        
        if (dialog.open() == Window.CANCEL) {
            return;
        }
        
        Object[] results = dialog.getResult();
        if (results == null || results.length == 0) {
            return;
        }
        
        if (results[0] instanceof IFile) {
            fMainText.setText(KotlinLaunchShortcut.getFileClassName((IFile) results[0]).asString());
        }
    }
}