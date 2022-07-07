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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.model.KotlinNature;
import org.jetbrains.kotlin.ui.launch.KotlinRuntimeConfigurator;

public class NewUnitWizard extends AbstractWizard<NewUnitWizardPage> {
    
    private static final String TITLE_FORMAT = "Kotlin %s File";
    private static final String DESCRIPTION_FORMAT = "Create a new Kotlin %s file";
    
    private static final String DEFAULT_FILE_NAME = "";
    private static final String DEFAULT_PACKAGE_NAME = "";
    private static final String DEFAULT_TYPE_BODY = "";
    private static final String PACKAGE_FORMAT = "package %s\n\n";
    
    private final WizardType type;

    private boolean isDynamicType;
    
    public NewUnitWizard() {
        this(WizardType.NONE);
        isDynamicType = true;
    }
    
    public NewUnitWizard(WizardType type) {
        this.type = type;
    }
    
    @Override
    public boolean performFinish() {
        NewUnitWizardPage wizardPage = getWizardPage();
        WizardType finalType;
        if(isDynamicType) {
            finalType = wizardPage.getType();
        } else {
            finalType = type;
        }
        String contents = createPackageHeader() + createTypeBody(finalType);
        
        IFile kotlinSourceFile;
        try {
            kotlinSourceFile = createKotlinSourceFile(
                    wizardPage.getSourceDir(), 
                    wizardPage.getPackageFragment(), 
                    wizardPage.getUnitName(), 
                    contents, 
                    getShell(), 
                    getContainer());
        } catch (InvocationTargetException e) {
            MessageDialog.openError(getShell(), AbstractWizard.ERROR_MESSAGE, e.getMessage());
            return false;
        } catch (InterruptedException e) {
            return false;
        }
        
        if (kotlinSourceFile != null) {
            selectAndRevealResource(kotlinSourceFile);
            openFile(kotlinSourceFile);
        }
        
        return true;
    }
    
    protected static void openFile(IFile file) {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        try {
            if (window != null) {
                IWorkbenchPage page = window.getActivePage();
                
                if (page != null) {
                    IDE.openEditor(page, file, true);
                }
            }
        } catch (PartInitException e) {
            KotlinLogger.logAndThrow(e);
        }
    }
    
    @Override
    protected String getPageTitle() {
        return String.format(TITLE_FORMAT, type.getWizardTypeName());
    }
    
    @Override
    protected NewUnitWizardPage createWizardPage() {
        return new NewUnitWizardPage(getPageTitle(), String.format(DESCRIPTION_FORMAT,
                type.getWizardTypeName().toLowerCase()), DEFAULT_FILE_NAME, getStructuredSelection(), isDynamicType);
    }
    
    @Nullable
    public static IFile createKotlinSourceFile(
            @NotNull IPackageFragmentRoot root,
            @NotNull IPackageFragment packageFragment,
            @NotNull String fileName,
            @NotNull String contents,
            @NotNull Shell shell,
            @NotNull IRunnableContext runnableContext) throws InvocationTargetException, InterruptedException {
        FileCreationOp operation = new FileCreationOp(root, packageFragment, fileName, contents, shell);
        runnableContext.run(true, true, operation);
        
        IProject project = root.getJavaProject().getProject();
        
        KotlinNature.Companion.addNature(project);
        KotlinRuntimeConfigurator.Companion.suggestForProject(project);
        
        return operation.getResult();
    }
    
    private String createTypeBody(WizardType finalType) {
        if (finalType == WizardType.NONE) {
            return DEFAULT_TYPE_BODY;
        }
        
        return String.format(finalType.getFileBodyFormat(), FileCreationOp.getSimpleUnitName(getWizardPage().getUnitName()));
    }
    
    private String createPackageHeader() {
        String pckg = getWizardPage().getPackageFragment().getElementName();
        if (pckg.isEmpty()) {
            return DEFAULT_PACKAGE_NAME;
        }
        
        return String.format(PACKAGE_FORMAT, pckg);
    }
    
}
