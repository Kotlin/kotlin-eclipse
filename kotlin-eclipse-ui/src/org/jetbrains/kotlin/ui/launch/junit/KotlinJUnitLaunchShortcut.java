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
import org.eclipse.debug.core.ILaunchConfiguration;
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
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil;

public class KotlinJUnitLaunchShortcut extends JUnitLaunchShortcut {
    public static final String KOTLIN_JUNIT_LAUNCH_ID = "org.jetbrains.kotlin.ui.launch.junit";
    
    @Override
    public void launch(IEditorPart editor, String mode) {
        IJavaElement eclipseElement = resolveToEclipseElement(editor);
        if (eclipseElement != null) {
            launch(eclipseElement, mode);
        } else {
            showNoTestsFoundDialog();
        }
    }
    
    @Override
    public void launch(ISelection selection, String mode) {
        IJavaElement eclipseElement = resolveToEclipseElement(selection);
        if (eclipseElement != null) {
            launch(eclipseElement, mode);
        } else {
            showNoTestsFoundDialog();
        }
    }
    
    @Nullable
    private IJavaElement resolveToEclipseElement(@NotNull IEditorPart editor) {
        IFile file = EditorUtil.getFile((AbstractTextEditor) editor);

        if (file != null) {
            IType eclipseType = KotlinJUnitLaunchUtils.getEclipseTypeForSingleClass(file);
            return eclipseType != null ? eclipseType : null;
        } else {
            KotlinLogger.logError("Failed to retrieve IFile from editor " + editor, null);
        }

        return null;
    }
    
    @Nullable
    private IJavaElement resolveToEclipseElement(@NotNull ISelection selection) {
        if (selection instanceof IStructuredSelection) {
            Object[] elements = ((IStructuredSelection) selection).toArray();
            if (elements.length == 1) {
                Object element = elements[0];
                if (element instanceof IFile) {
                    IFile file = (IFile) element;
                    return KotlinJUnitLaunchUtils.getEclipseTypeForSingleClass(file);
                }
            }
        }
        
        return null;
    }
    
    private void launch(@NotNull IJavaElement eclipseElement, @NotNull String mode) {
        try {
            ILaunchConfigurationWorkingCopy temporary = createLaunchConfiguration(eclipseElement);
            DebugUITools.launch(temporary, mode);
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
    }
    
    @Override
    public ILaunchConfiguration[] getLaunchConfigurations(final IEditorPart editor) {
        try {
            IJavaElement eclipseType = resolveToEclipseElement(editor);
            if (eclipseType != null) {
                return new ILaunchConfiguration[] { createLaunchConfiguration(eclipseType) };
            } 
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
        
        return null;
    }
    
    private void showNoTestsFoundDialog() {
        MessageDialog.openInformation(JUnitPlugin.getActiveWorkbenchShell(), 
                JUnitMessages.JUnitLaunchShortcut_dialog_title, JUnitMessages.JUnitLaunchShortcut_message_notests);
    }
}