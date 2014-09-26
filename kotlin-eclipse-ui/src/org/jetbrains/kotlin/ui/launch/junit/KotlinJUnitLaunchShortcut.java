/*******************************************************************************
 * Copyright 2010-2014 JetBrains s.r.o.
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
 *******************************************************************************/
package org.jetbrains.kotlin.ui.launch.junit;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.junit.ui.JUnitMessages;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.junit.launcher.JUnitLaunchShortcut;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.core.launch.KotlinJUnitLaunchConfigurationDelegate;
import org.jetbrains.kotlin.core.log.KotlinLogger;

public class KotlinJUnitLaunchShortcut extends JUnitLaunchShortcut {
    @Override
    public void launch(IEditorPart editor, String mode) {
    }
    
    @Override
    public void launch(ISelection selection, String mode) {
        if (selection instanceof IStructuredSelection) {
            Object[] elements = ((IStructuredSelection) selection).toArray();
            if (elements.length == 1) {
                Object element = elements[0];
                if (element instanceof IFile) {
                    launch((IFile) element, mode);
                }
            } else {
                showNoTestsFoundDialog();
            }
        }
    }
    
    private void launch(@NotNull IFile file, String mode) {
        try {
            IType eclipseType = KotlinJUnitLaunchableTester.getEclipseTypeForSingleClass(file);
            if (eclipseType != null) {
                ILaunchConfigurationWorkingCopy temporary = createLaunchConfiguration(eclipseType);
                DebugUITools.launch(temporary, mode);
            }
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
    }
    
    @Override
    protected ILaunchConfigurationWorkingCopy createLaunchConfiguration(IJavaElement element) throws CoreException {
        ILaunchConfigurationWorkingCopy jUnitLaunchConfiguration = super.createLaunchConfiguration(element);
        
        ILaunchConfigurationWorkingCopy kotlinLaunchDelegate = getLaunchConfigurationType()
                .newInstance(null, jUnitLaunchConfiguration.getName());
        kotlinLaunchDelegate.setAttributes(jUnitLaunchConfiguration.getAttributes());
        
        return kotlinLaunchDelegate;
    }
    
    private static ILaunchConfigurationType getLaunchConfigurationType() {
        return DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationType(
                KotlinJUnitLaunchConfigurationDelegate.LAUNCH_CONFIGURATION_TYPE_ID);
    }
    
    private void showNoTestsFoundDialog() {
        MessageDialog.openInformation(JUnitPlugin.getActiveWorkbenchShell(), 
                JUnitMessages.JUnitLaunchShortcut_dialog_title, JUnitMessages.JUnitLaunchShortcut_message_notests);
    }
}
