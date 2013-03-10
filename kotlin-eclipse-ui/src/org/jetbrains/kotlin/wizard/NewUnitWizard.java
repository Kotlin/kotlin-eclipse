package org.jetbrains.kotlin.wizard;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

public class NewUnitWizard extends Wizard implements INewWizard {

	private IWorkbench workbench;
	private IStructuredSelection selection;
	
	private NewUnitWizardPage page;
	
	private String title = "Kotlin Source File";
	private String description = "Create a new Kotlin souce file";
	private String defaultUnitName = "";
	private String contents = "";
	
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.workbench = workbench;
		this.selection = selection;
	}

	@Override
	public boolean performFinish() {
		FileCreationOp op = new FileCreationOp(page.getSourceDir(), 
                page.getPackageFragment(), page.getUnitName(), 
                false, contents, getShell());
	
		try {
            getContainer().run(true, true, op);
        } 
        catch (InvocationTargetException e) {
            e.printStackTrace();
            return false;
        } 
        catch (InterruptedException e) {
            return false;
        }
        BasicNewResourceWizard.selectAndReveal(op.getResult(), 
                workbench.getActiveWorkbenchWindow());
     
		return true;
	}
	
	@Override
	public void addPages() {
		super.addPages();
		
		if (page == null) {
			page = new NewUnitWizardPage(title, description, defaultUnitName);
		}
		addPage(page);
	}

}
