package org.jetbrains.kotlin.ui.launch;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.junit.ui.JUnitMessages;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.junit.launcher.JUnitLaunchShortcut;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.launch.KotlinJUnitLaunchConfigurationDelegate;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.model.KotlinJavaManager;

public class KotlinJUnitLaunchShortcut extends JUnitLaunchShortcut {
    @Override
    public void launch(IEditorPart editor, String mode) {
    }
    
    @Override
    public void launch(ISelection selection, String mode) {
        if (selection instanceof IStructuredSelection) {
            Object[] elements = ((IStructuredSelection) selection).toArray();
            if (elements.length == 1) {
                Object element = elements[0];
                if (element instanceof IFile) {
                    launch((IFile) element, mode);
                }
            } else {
                showNoTestsFoundDialog();
            }
        }
    }
    
    private void launch(@NotNull IFile file, String mode) {
        try {
            IType eclipseType = findTypeFor(file);
            if (eclipseType != null) {
                ILaunchConfigurationWorkingCopy temporary = createLaunchConfiguration(eclipseType);
                DebugUITools.launch(temporary, mode);
            }
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
    }
    
    @Override
    protected ILaunchConfigurationWorkingCopy createLaunchConfiguration(IJavaElement element) throws CoreException {
        ILaunchConfigurationWorkingCopy jUnitLaunchConfiguration = super.createLaunchConfiguration(element);
        
        ILaunchConfigurationWorkingCopy kotlinLaunchDelegate = getLaunchConfigurationType()
                .newInstance(null, jUnitLaunchConfiguration.getName());
        kotlinLaunchDelegate.setAttributes(jUnitLaunchConfiguration.getAttributes());
        
        return kotlinLaunchDelegate;
    }
    
    private static ILaunchConfigurationType getLaunchConfigurationType() {
        return DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationType(
                KotlinJUnitLaunchConfigurationDelegate.LAUNCH_CONFIGURATION_TYPE_ID);
    }

    @Nullable
    private IType findTypeFor(@NotNull IFile file) {
        JetFile jetFile = (JetFile) KotlinPsiManager.INSTANCE.getParsedFile(file);
        JetClass jetClass = findTopMostClass(jetFile);
        if (jetClass != null) {
            return KotlinJavaManager.INSTANCE.findEclipseType(jetClass, JavaCore.create(file.getProject()));
        }
        
        return null;
    }
    
    @Nullable
    private JetClass findTopMostClass(@NotNull JetFile jetFile) {
        for (JetDeclaration declaration : jetFile.getDeclarations()) {
            if (declaration instanceof JetClass) {
                return (JetClass) declaration;
            }
        }
        
        return null;
    }
    
    private void showNoTestsFoundDialog() {
        MessageDialog.openInformation(JUnitPlugin.getActiveWorkbenchShell(), 
                JUnitMessages.JUnitLaunchShortcut_dialog_title, JUnitMessages.JUnitLaunchShortcut_message_notests);
    }
}
