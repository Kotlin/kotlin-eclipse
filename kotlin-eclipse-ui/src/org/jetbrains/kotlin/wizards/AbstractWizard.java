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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.model.KotlinNature;

public abstract class AbstractWizard<WP extends AbstractWizardPage> extends Wizard implements INewWizard {

    public static final String ERROR_MESSAGE = "Error";

    private IWorkbench workbench;
    private IStructuredSelection selection;
    private WP page;

    public IWorkbench getWorkbench() {
        return workbench;
    }

    public IStructuredSelection getStructuredSelection() {
        return selection;
    }

    public WP getWizardPage() {
        return page;
    }

    protected abstract String getPageTitle();

    protected abstract WP createWizardPage();

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

    protected static void selectAndRevealResource(IResource resource) {
        BasicNewResourceWizard.selectAndReveal(resource, PlatformUI.getWorkbench().getActiveWorkbenchWindow());
    }

    protected static void addKotlinNatureToProject(IProject project) {
        try {
            KotlinNature.addNature(project);
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
    }

    protected static void addKotlinBuilderToProject(IProject project) {
        try {
            KotlinNature.addBuilder(project);
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
    }

}