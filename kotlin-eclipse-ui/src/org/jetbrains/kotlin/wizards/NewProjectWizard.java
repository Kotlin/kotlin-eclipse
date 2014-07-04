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

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

public class NewProjectWizard extends Wizard implements INewWizard {

    private static final String TITLE = "Kotlin project";
    private static final String DESCRIPTION = "Create a new Kotlin project";

    private IWorkbench workbench;
    private NewProjectWizardPage page;

    public NewProjectWizard() {

    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        this.workbench = workbench;
        this.setWindowTitle(TITLE);
    }

    @Override
    public boolean performFinish() {
        ProjectCreationOp op = new ProjectCreationOp();

        BasicNewResourceWizard.selectAndReveal(op.getResult(), workbench.getActiveWorkbenchWindow());

        return true;
    }

    @Override
    public void addPages() {
        super.addPages();

        if (page == null) {
            page = new NewProjectWizardPage(TITLE, DESCRIPTION);
        }

        addPage(page);
    }

}
