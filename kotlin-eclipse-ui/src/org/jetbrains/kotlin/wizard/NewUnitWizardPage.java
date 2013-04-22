package org.jetbrains.kotlin.wizard;

import static org.eclipse.jdt.internal.ui.refactoring.nls.SourceContainerDialog.getSourceContainer;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
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
import org.eclipse.ui.dialogs.SelectionDialog;

public class NewUnitWizardPage extends WizardPage implements IWizardPage {

    private String unitName;
    private String packageName = "";
    private IPackageFragmentRoot sourceDir;
    private IPackageFragment packageFragment;
    private final IStructuredSelection selection;

    private final String illegalUnitNameMessage = "Please enter a legal compilation unit name.";
    private final String selectSourceFolderMessage = "Please select a source folder";
    private final String illegalPackageNameMessage = "Please enter a legal package name";
    private final String unitExistsMessage = "File already exists";

    protected NewUnitWizardPage(String title, String description, String defaultUnitName, IStructuredSelection selection) {
        super(title);
        super.setTitle(title);
        super.setDescription(description);
        this.selection = selection;
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

        setPageComplete(false);
    }
    
    public IProject getProject() {
        if (sourceDir != null) {
            return sourceDir.getJavaProject().getProject();
        } else {
            return null;
        }
    }
    
    private void createControls(Composite composite) {
        Text folder = createFolderField(composite);
        folder.setText(getFolderFromSelection());
        
        Text pkg = createPackageField(composite);
        pkg.setText(getPackageFromSelection());

        Label separator = new Label(composite, SWT.HORIZONTAL | SWT.SEPARATOR);
        GridData sgd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        sgd.horizontalSpan = 4;
        separator.setLayoutData(sgd);

        Text name = createNameField(composite);
        name.forceFocus();
    }

    private Text createNameField(Composite composite) {
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
                setFormErrorMessage();
            }
        });

        new Label(composite, SWT.NONE);

        return name;
    }

    private Text createFolderField(Composite composite) {
        Label folderLabel = new Label(composite, SWT.LEFT | SWT.WRAP);
        folderLabel.setText("Source folder: ");
        GridData flgd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        flgd.horizontalSpan = 1;
        folderLabel.setLayoutData(flgd);

        final Text folder = new Text(composite, SWT.SINGLE | SWT.BORDER);
        GridData fgd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        fgd.horizontalSpan = 2;
        fgd.grabExcessHorizontalSpace = true;
        folder.setLayoutData(fgd);
        if (sourceDir != null) {
            String folderName = sourceDir.getPath().toPortableString();
            folder.setText(folderName);
        }
        folder.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                setSourceDir(folder.getText());
                setFormErrorMessage();
            }

            private void setSourceDir(String folderName) {
                try {
                    sourceDir = null;
                    for (IJavaProject jp : JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()).getJavaProjects()) {
                        for (IPackageFragmentRoot pfr : jp.getPackageFragmentRoots()) {
                            if (pfr.getPath().toPortableString().equals(folderName)) {
                                sourceDir = pfr;
                                return;
                            }
                        }
                    }
                } catch (JavaModelException jme) {
                    jme.printStackTrace();
                }
            }
        });

        Button selectFolder = new Button(composite, SWT.PUSH);
        selectFolder.setText("Browse...");
        GridData sfgd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        sfgd.horizontalSpan = 1;
        selectFolder.setLayoutData(sfgd);
        selectFolder.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                IPackageFragmentRoot pfr = getSourceContainer(getShell(), ResourcesPlugin.getWorkspace().getRoot(),
                        sourceDir);
                if (pfr != null) {
                    sourceDir = pfr;
                    String folderName = sourceDir.getPath().toPortableString();
                    folder.setText(folderName);
                    packageFragment = sourceDir.getPackageFragment(packageName);
                }
                setFormErrorMessage();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });

        return folder;
    }

    private Text createPackageField(Composite composite) {
        Label packageLabel = new Label(composite, SWT.LEFT | SWT.WRAP);
        packageLabel.setText("Package: ");
        GridData plgd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        plgd.horizontalSpan = 1;
        packageLabel.setLayoutData(plgd);

        final Text pkg = new Text(composite, SWT.SINGLE | SWT.BORDER);
        GridData pgd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        pgd.horizontalSpan = 2;
        pgd.grabExcessHorizontalSpace = true;
        pkg.setLayoutData(pgd);
        pkg.setText(packageName);
        pkg.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                packageName = pkg.getText();
                setFormErrorMessage();
            }
        });

        Button selectPackage = new Button(composite, SWT.PUSH);
        selectPackage.setText("Browse...");
        GridData spgd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        spgd.horizontalSpan = 1;
        selectPackage.setLayoutData(spgd);
        selectPackage.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (sourceDir == null) {
                    MessageDialog.openWarning(getShell(), "No Source Folder", selectSourceFolderMessage);
                } else {
                    SelectionDialog dialog;
                    Object result = null;
                    try {
                        dialog = JavaUI.createPackageDialog(getShell(), sourceDir);
                        dialog.setTitle("Package Selection");
                        dialog.setMessage("Select a package:");
                        dialog.open();
                        if (dialog.getResult() != null) {
                            result = dialog.getResult()[0];
                        }
                    } catch (JavaModelException jme) {
                        jme.printStackTrace();
                    }
                    if (result != null) {
                        packageName = ((IPackageFragment) result).getElementName();
                        pkg.setText(packageName);
                        if (sourceDir != null) {
                            packageFragment = sourceDir.getPackageFragment(packageName);
                        }
                    }
                    setFormErrorMessage();
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });

        return pkg;
    }
    
    private String getFolderFromSelection() {
        String defaultFolder = "";
        
        if (selection.isEmpty()) {
            return defaultFolder;
        }
        
        Object selectedObject = selection.getFirstElement();
        
        if (selectedObject instanceof IJavaElement) {
            IJavaElement selectedJavaElement = (IJavaElement) selectedObject;
            switch (selectedJavaElement.getElementType()) {
                case IJavaElement.JAVA_PROJECT:
                    return getDefaultSrcByProject((IJavaProject) selectedJavaElement);
                    
                case IJavaElement.PACKAGE_FRAGMENT_ROOT:
                    return selectedJavaElement.getPath().toOSString();
                    
                case IJavaElement.PACKAGE_FRAGMENT: case IJavaElement.COMPILATION_UNIT:
                    return selectedJavaElement.getPath().uptoSegment(2).toOSString();
            }
        } else if (selectedObject instanceof IResource) {
            IResource selectedResource = (IResource) selectedObject;
            switch (selectedResource.getType()) {
                case IResource.FOLDER:
                    return getDefaultSrcByProject(JavaCore.create(selectedResource.getProject()));
                    
                case IResource.FILE:
                    return selectedResource.getFullPath().uptoSegment(2).toOSString();
            }
        } 
        
        return defaultFolder;
    }
    
    private String getPackageFromSelection() {
        String defaultPackage = "";
        
        if (selection.isEmpty()) {
            return defaultPackage;
        }
        
        Object selectedObject = selection.getFirstElement();
        
        if (selectedObject instanceof IJavaElement) {
            IJavaElement selectedJavaElement = (IJavaElement) selectedObject;
            switch (selectedJavaElement.getElementType()) {
                case IJavaElement.PACKAGE_FRAGMENT:  
                    return selectedJavaElement.getElementName();
                    
                case IJavaElement.COMPILATION_UNIT:
                    try {
                        return selectedJavaElement.getJavaProject().
                                findPackageFragment(selectedJavaElement.getPath().makeAbsolute().removeLastSegments(1)).
                                getElementName();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
            }
        } else if (selectedObject instanceof IResource) {
            IResource selectedResource = (IResource) selectedObject;
            switch (selectedResource.getType()) {
                case IResource.FILE:
                    try {
                        return JavaCore.create(selectedResource.getProject()).
                                findPackageFragment(selectedResource.getFullPath().makeAbsolute().removeLastSegments(1)).
                                getElementName();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
            }
        } 
        
        return defaultPackage;
    }
    
    private String getDefaultSrcByProject(IJavaProject javaProject) {
        String destFolder = javaProject.getPath().toOSString();
        
        IClasspathEntry[] classpathEntries = null;
        try {
            classpathEntries = javaProject.getRawClasspath();
        } catch (JavaModelException e) {
            e.printStackTrace();
            
            return destFolder;
        }
        
        for (IClasspathEntry classpathEntry : classpathEntries) {
            if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
                destFolder += File.separatorChar + classpathEntry.getPath().segment(1);
                break;
            }
        }
        
        return destFolder;
    }

    private void setFormErrorMessage() {
        boolean pageCompleteStatus = false;
        if (sourceDir != null && packageNameIsLegal()) {
            packageFragment = sourceDir.getPackageFragment(packageName);
        }
        if (sourceDir == null) {
            setErrorMessage(selectSourceFolderMessage);
        } else if (!packageNameIsLegal()) {
            setErrorMessage(illegalPackageNameMessage);
        } else if (!unitIsNameLegal()) {
            setErrorMessage(illegalUnitNameMessage);
        } else if (unitExists()) {
            setErrorMessage(unitExistsMessage);
        } else {
            setErrorMessage(null);
            pageCompleteStatus = true;
        }
        setPageComplete(pageCompleteStatus);
    }

    private boolean unitExists() {
        IPath path = packageFragment.getPath().append(FileCreationOp.getCompilationUnitName(unitName));
        IProject project = sourceDir.getJavaProject().getProject();
        IFile result = project.getFile(path.makeRelativeTo(project.getFullPath()));
        return result.exists();
    }

    private boolean packageNameIsLegal(String packageName) {
        return packageName.isEmpty() || packageName.matches("^[a-z_]\\w*(\\.[a-z_]\\w*)*$");
    }

    private boolean packageNameIsLegal() {
        return packageName != null && packageNameIsLegal(packageName);
    }

    private boolean unitIsNameLegal() {
        return unitName != null && unitIsNameLegal(unitName);
    }

    private boolean unitIsNameLegal(String unitName) {
        return !unitName.trim().isEmpty();
    }

    IPackageFragment getPackageFragment() {
        return packageFragment;
    }

    IPackageFragmentRoot getSourceDir() {
        return sourceDir;
    }

    String getUnitName() {
        return unitName;
    }
}
