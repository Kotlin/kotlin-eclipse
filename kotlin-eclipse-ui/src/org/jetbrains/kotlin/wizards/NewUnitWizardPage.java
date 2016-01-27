/*******************************************************************************
 * Copyright 2000-2016 JetBrains s.r.o.
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

import static org.eclipse.jdt.internal.ui.refactoring.nls.SourceContainerDialog.getSourceContainer;
import static org.jetbrains.kotlin.wizards.FileCreationOp.fileExists;
import static org.jetbrains.kotlin.wizards.FileCreationOp.makeFile;
import static org.jetbrains.kotlin.wizards.SWTWizardUtils.createButton;
import static org.jetbrains.kotlin.wizards.SWTWizardUtils.createEmptySpace;
import static org.jetbrains.kotlin.wizards.SWTWizardUtils.createLabel;
import static org.jetbrains.kotlin.wizards.SWTWizardUtils.createSeparator;
import static org.jetbrains.kotlin.wizards.SWTWizardUtils.createText;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.jetbrains.kotlin.core.log.KotlinLogger;

public class NewUnitWizardPage extends AbstractWizardPage {
    
    private static final String DEFAULT_SOURCE_FOLDER = "";
    private static final String DEFAULT_PACKAGE = "";
    
    private static final String NAME_LABEL_TITLE = "Na&me";
    private static final String SOURCE_FOLDER_LABEL_TITLE = "Source fol&der";
    private static final String PACKAGE_LABEL_TITLE = "Pac&kage";
    
    private static final String ILLEGAL_UNIT_NAME_MESSAGE = "Please enter a legal compilation unit name";
    private static final String SELECT_SOURCE_FOLDER_MESSAGE = "Please select a source folder";
    private static final String ILLEGAL_PACKAGE_NAME_MESSAGE = "Please enter a legal package name";
    private static final String UNIT_EXISTS_MESSAGE = "File already exists";
    
    private static final String JAVA_IDENTIFIER_REGEXP = "[a-zA-Z_]\\w*";
    
    private String unitName;
    private String packageName;
    private IPackageFragmentRoot sourceDir;
    private IPackageFragment packageFragment;
    private Text nameField = null;
    private final IStructuredSelection selection;
    
    protected NewUnitWizardPage(String title, String description, String unitName, IStructuredSelection selection) {
        super(title, description);
        
        this.selection = selection;
        this.unitName = unitName;
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
    
    public IProject getProject() {
        if (sourceDir != null) {
            return sourceDir.getJavaProject().getProject();
        } else {
            return null;
        }
    }
    
    @Override
    protected void createControls(Composite parent) {
        createSourceFolderField(parent);
        createPackageField(parent);
        
        createSeparator(parent);
        
        nameField = createNameField(parent);
    }
    
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            if (nameField != null) {
                nameField.setFocus();
            }
        }
    }

    private Text createNameField(Composite parent) {
        createLabel(parent, NAME_LABEL_TITLE);
        
        final Text name = createText(parent, unitName);
        name.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                unitName = name.getText();
                validate();
            }
        });
        
        createEmptySpace(parent);
        
        return name;
    }
    
    private void setSourceDirByFolderName(String srcFolder) {
        try {
            sourceDir = null;
            for (IJavaProject jp : JavaCore.create(getWorkspaceRoot()).getJavaProjects()) {
                for (IPackageFragmentRoot pfr : jp.getPackageFragmentRoots()) {
                    if (pfr.getPath().toPortableString().equals(srcFolder)) {
                        sourceDir = pfr;
                        return;
                    }
                }
            }
        } catch (JavaModelException jme) {
            KotlinLogger.logAndThrow(jme);
        }
    }
    
    private Text createSourceFolderField(Composite parent) {
        createLabel(parent, SOURCE_FOLDER_LABEL_TITLE);
        
        IPackageFragmentRoot srcFolder = WizardUtilsKt.getSourceFolderBySelection(selection);
        String sourceFolderFromSelection = srcFolder != null ? srcFolder.getPath().toOSString() : DEFAULT_SOURCE_FOLDER;
        sourceDir = srcFolder;
        
        final Text folder = createText(parent, sourceFolderFromSelection);
        folder.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                setSourceDirByFolderName(folder.getText());
                validate();
            }
        });
        
        createButton(parent, BROWSE_BUTTON_TITLE, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                IPackageFragmentRoot pfr = getSourceContainer(getShell(), getWorkspaceRoot(), sourceDir);
                if (pfr != null) {
                    sourceDir = pfr;
                    String folderName = sourceDir.getPath().toPortableString();
                    folder.setText(folderName);
                    packageFragment = sourceDir.getPackageFragment(packageName);
                }
                
                validate();
            }
        });
        
        return folder;
    }
    
    private Text createPackageField(Composite parent) {
        createLabel(parent, PACKAGE_LABEL_TITLE);
        
        IPackageFragment fragment = WizardUtilsKt.getPackageBySelection(selection);
        String packageFromSelection = fragment != null ? fragment.getElementName() : DEFAULT_PACKAGE;
        packageName = packageFromSelection;
        
        final Text pkg = createText(parent, packageFromSelection);
        pkg.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                packageName = pkg.getText();
                validate();
            }
        });
        
        createButton(parent, BROWSE_BUTTON_TITLE, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (sourceDir == null) {
                    MessageDialog.openWarning(getShell(), "No Source Folder", SELECT_SOURCE_FOLDER_MESSAGE);
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
                        KotlinLogger.logAndThrow(jme);
                    }
                    if (result != null) {
                        packageName = ((IPackageFragment) result).getElementName();
                        pkg.setText(packageName);
                        if (sourceDir != null) {
                            packageFragment = sourceDir.getPackageFragment(packageName);
                        }
                    }
                    validate();
                }
            }
        });
        
        return pkg;
    }
    
    @Override
    protected String createErrorMessage() {
        if (sourceDir != null && packageNameIsLegal()) {
            packageFragment = sourceDir.getPackageFragment(packageName);
        }
        
        if (sourceDir == null) {
            return SELECT_SOURCE_FOLDER_MESSAGE;
        } else if (!packageNameIsLegal()) {
            return ILLEGAL_PACKAGE_NAME_MESSAGE;
        } else if (!unitIsNameLegal()) {
            return ILLEGAL_UNIT_NAME_MESSAGE;
        } else if (resourceAlreadyExists()) {
            return UNIT_EXISTS_MESSAGE;
        } else {
            return null;
        }
    }
    
    @Override
    protected boolean resourceAlreadyExists() {
        return fileExists(makeFile(packageFragment, sourceDir, unitName));
    }
    
    private boolean packageNameIsLegal(String packageName) {
        return packageName.matches("^(|" + JAVA_IDENTIFIER_REGEXP + "(\\." + JAVA_IDENTIFIER_REGEXP + ")*)$");
    }
    
    private boolean packageNameIsLegal() {
        return packageName != null && packageNameIsLegal(packageName);
    }
    
    private boolean unitIsNameLegal() {
        return unitName != null && unitIsNameLegal(unitName);
    }
    
    private boolean unitIsNameLegal(String unitName) {
        return unitName.matches("^" + JAVA_IDENTIFIER_REGEXP + FileCreationOp.getExtensionRegexp() + "$");
    }
    
}
