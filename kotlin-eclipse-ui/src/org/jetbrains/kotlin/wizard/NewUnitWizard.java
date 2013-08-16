package org.jetbrains.kotlin.wizard;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.model.KotlinNature;

public class NewUnitWizard extends Wizard implements INewWizard {

    private IWorkbench workbench;
    private IStructuredSelection selection;
    private NewUnitWizardPage page;

    private final String title = "Kotlin Source File";
    private final String description = "Create a new Kotlin souce file";
    private final String defaultUnitName = "";
    private String contents = "";

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        this.workbench = workbench;
        this.selection = selection;
        this.setWindowTitle(title);
    }

    @Override
    public boolean performFinish() {
        contents = createPackageHeader();
        FileCreationOp op = new FileCreationOp(page.getSourceDir(), page.getPackageFragment(), page.getUnitName(),
                false, contents, getShell());

        try {
            getContainer().run(true, true, op);
        } catch (InvocationTargetException e) {
            MessageDialog.openError(getShell(), "Error", e.getMessage());
            return false;
        } catch (InterruptedException e) {
            return false;
        }
        
        try {
            KotlinNature.addNature(page.getProject());
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
        
        try {
            KotlinNature.addBuilder(page.getProject());
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
        
        BasicNewResourceWizard.selectAndReveal(op.getResult(), workbench.getActiveWorkbenchWindow());

        return true;
    }
    
    private String createPackageHeader() {
        String pckg = page.getPackageFragment().getElementName();
        if (pckg.isEmpty()) {
            return "";
        }
        
        return "package " + pckg;
    }

    @Override
    public void addPages() {
        super.addPages();

        if (page == null) {
            page = new NewUnitWizardPage(title, description, defaultUnitName, selection);
        }
        addPage(page);
    }

}
