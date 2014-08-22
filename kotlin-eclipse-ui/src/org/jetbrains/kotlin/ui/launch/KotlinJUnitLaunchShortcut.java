package org.jetbrains.kotlin.ui.launch;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.junit.launcher.JUnitLaunchShortcut;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFile;

public class KotlinJUnitLaunchShortcut extends JUnitLaunchShortcut {

    @Override
    public void launch(IEditorPart editor, String mode) {
//        IFile file = EditorUtil.getFile((AbstractTextEditor) editor);
//        JetClass jetClass = findTopMostClass((JetFile) KotlinPsiManager.INSTANCE.getParsedFile(file));
//        if (jetClass != null) {
//            
//        }
//        
//        System.out.println("From editor");
    }
    
    @Override
    public void launch(ISelection selection, String mode) {
        if (selection instanceof IStructuredSelection) {
            Object[] elements = ((IStructuredSelection) selection).toArray();
            if (elements.length == 1) {
                Object element = elements[0];
                if (element instanceof IFile) {
                    launch((IFile) element);
                }
            }
        }
    }
    
    private void launch(@NotNull IFile file) {
        
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
}
