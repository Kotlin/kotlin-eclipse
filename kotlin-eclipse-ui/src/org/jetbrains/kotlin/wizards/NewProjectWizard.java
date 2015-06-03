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


public class NewProjectWizard extends AbstractWizard<NewProjectWizardPage> {

    private static final String TITLE = "Kotlin project";
    private static final String DESCRIPTION = "Create a new Kotlin project";

    @Override
    public boolean performFinish() {
        NewProjectWizardPage page = getWizardPage();
        
        ProjectCreationOp op = new ProjectCreationOp(page.getProjectName(), page.getProjectLocation(), getShell());
        performOperation(op, getContainer(), getShell());
        
        addKotlinNatureToProject(op.getResult());
        addKotlinBuilderToProject(op.getResult());
        
        selectAndRevealResource(op.getResult());

        return true;
    }

    @Override
    protected String getPageTitle() {
        return TITLE;
    }

    @Override
    protected NewProjectWizardPage createWizardPage() {
        return new NewProjectWizardPage(TITLE, DESCRIPTION);
    }

}
