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
package org.jetbrains.kotlin.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.model.KotlinNature;

public abstract class AbstractWizard extends Wizard implements INewWizard {

    private static final String ERROR_MESSAGE = "Error";

    private IWorkbench workbench;
    private IStructuredSelection selection;
    private IWizardPage page;

    public IWorkbench getWorkbench() {
        return workbench;
    }

    public IStructuredSelection getStructuredSelection() {
        return selection;
    }

    public IWizardPage getWizardPage() {
        return page;
    }

    protected abstract String getPageTitle();

    protected abstract IWizardPage createWizardPage();

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        this.workbench = workbench;
        this.selection = selection;

        setWindowTitle(getPageTitle());
    }

    @Override
    public void addPages() {
        super.addPages();

        if (page == null) {
            page = createWizardPage();
        }

        addPage(page);
    }

    protected void selectAndRevealResource(IResource resource) {
        BasicNewResourceWizard.selectAndReveal(resource, getWorkbench().getActiveWorkbenchWindow());
    }

    protected boolean performOperation(IRunnableWithProgress operation) {
        try {
            getContainer().run(true, true, operation);
        } catch (InvocationTargetException e) {
            MessageDialog.openError(getShell(), ERROR_MESSAGE, e.getMessage());
            return false;
        } catch (InterruptedException e) {
            return false;
        }

        return true;
    }

    protected static void addNatureToProject(IProject project) {
        try {
            KotlinNature.addNature(project);
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
    }

    protected static void addBuilderToProject(IProject project) {
        try {
            KotlinNature.addBuilder(project);
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
    }

}