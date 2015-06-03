package org.jetbrains.kotlin.ui.commands.j2k;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISources;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.model.KotlinEnvironment;
import org.jetbrains.kotlin.j2k.J2kPackage;
import org.jetbrains.kotlin.j2k.JavaToKotlinTranslator;
import org.jetbrains.kotlin.ui.editors.KotlinEditor;
import org.jetbrains.kotlin.ui.editors.KotlinFormatAction;
import org.jetbrains.kotlin.ui.editors.KotlinOpenDeclarationAction;
import org.jetbrains.kotlin.wizards.NewUnitWizard;

import com.intellij.openapi.project.Project;

public class JavaToKotlinActionHandler extends AbstractHandler {
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        ISelection selection = HandlerUtil.getActiveMenuSelection(event);
        if (selection instanceof IStructuredSelection) {
            Object[] elements = ((IStructuredSelection) selection).toArray();
            Shell shell = HandlerUtil.getActiveShell(event);
            if (elements.length != 1) {
                MessageDialog.openError(shell, "Error", "Could not convert more than one file at once");
                return null;
            }
            
            if (elements[0] instanceof CompilationUnit) {
                CompilationUnit compilationUnit = (CompilationUnit) elements[0];
                IFile kotlinFile = convertToKotlin(compilationUnit, event);
                if (kotlinFile == null) {
                    MessageDialog.openError(shell, "Error", "Could not convert file to Kotlin");
                    return null;
                }
                
                openAndFormatFile(kotlinFile);
                deleteJavaFile(compilationUnit);
            } else {
                MessageDialog.openError(shell, "Error", "Could not convert to Kotlin non-java file");
                return null;
            }
        }
        
        return null;
    }
    
    @Override
    public void setEnabled(Object evaluationContext) {
        Object editorObject = HandlerUtil.getVariable(evaluationContext, ISources.ACTIVE_EDITOR_NAME);
        setBaseEnabled(editorObject instanceof JavaEditor && !(editorObject instanceof KotlinEditor));
    }
    
    private IFile convertToKotlin(@NotNull CompilationUnit compilationUnit, @NotNull ExecutionEvent event) {
        String contents = new String(compilationUnit.getContents());
        Project ideaProject = KotlinEnvironment.getEnvironment(compilationUnit.getJavaProject()).getProject();
        
        String translatedCode = JavaToKotlinTranslator.INSTANCE$.prettify(
                J2kPackage.translateToKotlin(contents, ideaProject));
        
        String elementName = compilationUnit.getElementName();
        return NewUnitWizard.createKotlinSourceFile(
                compilationUnit.getPackageFragmentRoot(), 
                (IPackageFragment) compilationUnit.getParent(),
                elementName.substring(0, elementName.length() - compilationUnit.getResource().getFileExtension().length() - 1),
                translatedCode, 
                HandlerUtil.getActiveShell(event), 
                PlatformUI.getWorkbench().getActiveWorkbenchWindow());
    }
    
    private void openAndFormatFile(@NotNull IFile kotlinFile) {
        IEditorPart editorPart = KotlinOpenDeclarationAction.openInEditor(kotlinFile);
        if (editorPart instanceof KotlinEditor) {
            new KotlinFormatAction((KotlinEditor) editorPart).run();
        }
    }
    
    private void deleteJavaFile(@NotNull CompilationUnit compilationUnit) {
        try {
            compilationUnit.delete(true, null);
        } catch (JavaModelException e) {
            KotlinLogger.logError("Cannot delete java file: " + compilationUnit.getElementName(), null);
        }        
    }
}
