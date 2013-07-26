package org.jetbrains.kotlin.ui.launch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaMainTab;
import org.eclipse.jdt.internal.ui.viewsupport.JavaUILabelProvider;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.dialogs.ListDialog;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.utils.ProjectUtils;

public class KotlinMainLauncherTab extends JavaMainTab implements ILaunchConfigurationTab {
    
    @Override
    public void handleSearchButtonSelected() {
        ListDialog dialog = new ListDialog(getShell());
        dialog.setBlockOnOpen(true);
        dialog.setMessage("Select a Kotlin file to run");
        dialog.setTitle("Choose Kotlin Compilation Unit");
        dialog.setContentProvider(new ArrayContentProvider());
        dialog.setLabelProvider(new JavaUILabelProvider());

        Collection<IFile> projectFiles = null;
        projectFiles = KotlinPsiManager.INSTANCE.getFilesByProject(fProjText.getText());
        
        List<IFile> mainFiles = new ArrayList<IFile>();
        for (IFile file : projectFiles) {
            if (ProjectUtils.hasMain(file)) {
                mainFiles.add(file);
            }
        }
        
        dialog.setInput(mainFiles);
        
        if (dialog.open() == Window.CANCEL) {
            return;
        }
        
        Object[] results = dialog.getResult();
        if (results == null || results.length == 0) {
            return;
        }
        
        if (results[0] instanceof IFile) {
            fMainText.setText(ProjectUtils.createPackageClassName(((IFile) results[0])).toString());
        }
    }
}