package org.jetbrains.kotlin.wizard;

import static org.eclipse.jdt.internal.ui.refactoring.nls.SourceContainerDialog.getSourceContainer;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.core.resources.ResourcesPlugin;

public class NewUnitWizardPage extends WizardPage implements IWizardPage {

	private String unitName;
	private String packageName = "";
	private IPackageFragmentRoot sourceDir;
    private IPackageFragment packageFragment;
    
    private IStructuredSelection selection;
    private IWorkbench workbench;
    private Text unitNameText;
	
	protected NewUnitWizardPage(String title, String description, String defaultUnitName) {
		super(title);
		super.setTitle(title);
		super.setDescription(description);
		unitName = defaultUnitName;
	}

	@Override
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);
		
		Composite composite = new Composite(parent, SWT.NONE);
        composite.setFont(parent.getFont());
        
        GridLayout layout = new GridLayout();
        layout.numColumns = 4;
        composite.setLayout(layout);
        
        createControls(composite);
        
        setControl(composite);
        
        setPageComplete(isComplete());
	}
	
	private void createControls(Composite composite) {
		Text folder = createFolderField(composite);
        Text pkg = createPackageField(composite);
        
        Label separator = new Label(composite, SWT.HORIZONTAL | SWT.SEPARATOR);
        GridData sgd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        sgd.horizontalSpan = 4;
        separator.setLayoutData(sgd);
        
        Text name = createNameField(composite);
        name.forceFocus();
	}
	
	Text createNameField(Composite composite) {
		Label nameLabel = new Label(composite, SWT.LEFT | SWT.WRAP);
        nameLabel.setText("Name: ");
        GridData lgd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        lgd.horizontalSpan = 1;
        nameLabel.setLayoutData(lgd);
        
        final Text name = new Text(composite, SWT.SINGLE | SWT.BORDER);
        GridData ngd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        ngd.horizontalSpan = 2;
        ngd.grabExcessHorizontalSpace = true;
        name.setLayoutData(ngd);
        name.setText(unitName);
        name.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                unitName = name.getText();
                if (!unitIsNameLegal()) {
                    setErrorMessage(getIllegalUnitNameMessage());
                }
                else if (sourceDir == null) {
                    setErrorMessage(getSelectSourceFolderMessage());
                }
                else if (!packageNameIsLegal()) {
                    setErrorMessage(getIllegalPackageNameMessage());
                }
                else {
                    setErrorMessage(null); 
                }
                setPageComplete(isComplete());
            }
        });
     
        unitNameText = name;
        
        new Label(composite, SWT.NONE);

        return name;
	}
	
	Text createFolderField(Composite composite) {
        Label folderLabel = new Label(composite, SWT.LEFT | SWT.WRAP);
        folderLabel.setText("Source folder: ");
        GridData flgd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        flgd.horizontalSpan = 1;
        folderLabel.setLayoutData(flgd);

        final Text folder = new Text(composite, SWT.SINGLE | SWT.BORDER);
        GridData fgd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        fgd.horizontalSpan = 2;
        fgd.grabExcessHorizontalSpace = true;
        folder.setLayoutData(fgd);
        if (sourceDir!=null) {
            String folderName = sourceDir.getPath().toPortableString();
            folder.setText(folderName);
        }        
        folder.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                setSourceDir(folder.getText());
                if (sourceDir != null && packageNameIsLegal()) {
                    packageFragment = sourceDir.getPackageFragment(packageName);
                }
                if (sourceDir==null) {
                    setErrorMessage(getSelectSourceFolderMessage());
                }
                else if (!packageNameIsLegal()) {
                    setErrorMessage(getIllegalPackageNameMessage());
                }
                else if (!unitIsNameLegal()) {
                    setErrorMessage(getIllegalUnitNameMessage());
                }
                else {
                    setErrorMessage(null);
                }
                setPageComplete(isComplete());
            }
            
            private void setSourceDir(String folderName) {
                try {
                    sourceDir = null;
                    for (IJavaProject jp: JavaCore.create(ResourcesPlugin.getWorkspace().getRoot())
                            .getJavaProjects()) {
                        for (IPackageFragmentRoot pfr: jp.getPackageFragmentRoots()) {
                            if (pfr.getPath().toPortableString().equals(folderName)) {
                                sourceDir = pfr;
                                return;
                            }
                        }
                    }
                }
                catch (JavaModelException jme) {
                    jme.printStackTrace();
                }
            }
        });
        
        Button selectFolder = new Button(composite, SWT.PUSH);
        selectFolder.setText("Browse...");
        GridData sfgd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        sfgd.horizontalSpan = 1;
        selectFolder.setLayoutData(sfgd);
        selectFolder.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                IPackageFragmentRoot pfr = getSourceContainer(getShell(), 
                        ResourcesPlugin.getWorkspace().getRoot(), sourceDir);
                if (pfr!=null) {
                    sourceDir = pfr;
                    String folderName = sourceDir.getPath().toPortableString();
                    folder.setText(folderName);
                    packageFragment = sourceDir.getPackageFragment(packageName);
                    setPageComplete(isComplete());
                }
                if (sourceDir == null) {
                    setErrorMessage(getSelectSourceFolderMessage());
                }
                else if (!packageNameIsLegal()) {
                    setErrorMessage(getIllegalPackageNameMessage());
                }
                else if (!unitIsNameLegal()) {
                    setErrorMessage(getIllegalUnitNameMessage());
                }
                else {
                    setErrorMessage(null);
                }
            }
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {}
        });
        
        return folder;
    }
	
Text createPackageField(Composite composite) {
        
        Label packageLabel = new Label(composite, SWT.LEFT | SWT.WRAP);
        packageLabel.setText("Package: ");
        GridData plgd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        plgd.horizontalSpan = 1;
        packageLabel.setLayoutData(plgd);

        final Text pkg = new Text(composite, SWT.SINGLE | SWT.BORDER);
        GridData pgd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        pgd.horizontalSpan = 2;
        pgd.grabExcessHorizontalSpace = true;
        pkg.setLayoutData(pgd);
        pkg.setText(packageName);
        pkg.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                packageName = pkg.getText();
                if (sourceDir != null && packageNameIsLegal()) {
                    packageFragment = sourceDir.getPackageFragment(packageName);
                }
                if (!packageNameIsLegal()) {
                    setErrorMessage(getIllegalPackageNameMessage());
                }
                else if (sourceDir == null) {
                    setErrorMessage(getSelectSourceFolderMessage());
                }
                else if (!unitIsNameLegal()) {
                    setErrorMessage(getIllegalUnitNameMessage());
                }
                else {
                    setErrorMessage(null);
                }
                setPageComplete(isComplete());
            }
        });
        
        Button selectPackage = new Button(composite, SWT.PUSH);
        selectPackage.setText("Browse...");
        GridData spgd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        spgd.horizontalSpan = 1;
        selectPackage.setLayoutData(spgd);
        selectPackage.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (sourceDir == null) {
                    MessageDialog.openWarning(getShell(), "No Source Folder", 
                            getSelectSourceFolderMessage());
                }
                else {
                    PackageSelectionDialog dialog = new PackageSelectionDialog(getShell(), sourceDir);
                    dialog.setMultipleSelection(false);
                    dialog.setTitle("Package Selection");
                    dialog.setMessage("Select a package:");
                    dialog.open();
                    Object result = dialog.getFirstResult();
                    if (result!=null) {
                        packageName = ((IPackageFragment) result).getElementName();
                        pkg.setText(packageName);
                        if (sourceDir!=null) {
                            packageFragment = sourceDir.getPackageFragment(packageName);
                        }
                        setPageComplete(isComplete());
                    }
                    if (!packageNameIsLegal()) {
                        setErrorMessage(getIllegalPackageNameMessage());
                    }
                    else if (sourceDir == null) {
                        setErrorMessage(getSelectSourceFolderMessage());
                    }
                    else if (!unitIsNameLegal()) {
                        setErrorMessage(getIllegalUnitNameMessage());
                    }
                    else {
                        setErrorMessage(null);
                    }
                }
            }
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {}
        });
        
        return pkg;
    }
	
	private boolean packageNameIsLegal(String packageName) {
        return packageName.isEmpty() || 
            packageName.matches("^[a-z_]\\w*(\\.[a-z_]\\w*)*$");
    }
    
    private boolean packageNameIsLegal() {
        return packageName != null &&
                packageNameIsLegal(packageName);
    }
    
    private boolean unitIsNameLegal() {
        return unitName != null && 
                unitIsNameLegal(unitName);
    }
    
    boolean isComplete() {
        return packageNameIsLegal() && unitIsNameLegal() &&
                sourceDir!=null &&
                sourceDir.getPackageFragment(packageFragment.getElementName())
                        .equals(packageFragment);
    }

    private boolean unitIsNameLegal(String unitName) {
        return unitName.matches("\\w+");
    }
    
    private String getIllegalUnitNameMessage() {
        return "Please enter a legal compilation unit name.";
    }
    
    private String getSelectSourceFolderMessage() {
        return "Please select a source folder";
    }
    
    String getIllegalPackageNameMessage() {
        return "Please enter a legal package name.";
    }
    
    public IPackageFragment getPackageFragment() {
        return packageFragment;
    }
    
    public IPackageFragmentRoot getSourceDir() {
        return sourceDir;
    }
    
    public String getUnitName() {
        return unitName;
    }

}
