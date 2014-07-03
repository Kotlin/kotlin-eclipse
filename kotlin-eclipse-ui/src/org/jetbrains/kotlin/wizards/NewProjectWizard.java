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
