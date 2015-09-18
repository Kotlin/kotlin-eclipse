package org.jetbrains.kotlin.ui.navigation;

import java.io.File;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.core.ClassFile;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.filesystem.KotlinLightClassManager;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil;
import org.jetbrains.kotlin.psi.JetElement;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.ui.editors.KotlinClassFileEditor;
import org.jetbrains.kotlin.ui.editors.KotlinClassFileEditorInput;
import org.jetbrains.kotlin.ui.editors.KotlinEditor;

// Seeks Kotlin editor by IJavaElement
public class KotlinOpenEditor {
	@Nullable
	public static IEditorPart openKotlinEditor(@NotNull IJavaElement element, boolean activate) {
	    File lightClass = element.getResource().getFullPath().toFile();
	    List<JetFile> sourceFiles = KotlinLightClassManager.getInstance(element.getJavaProject()).getSourceFiles(lightClass);
	    JetFile navigationFile = NavigationPackage.findNavigationFileFromSources(element, sourceFiles);
	    
	    IFile kotlinFile;
	    if (navigationFile != null) {
	        kotlinFile = KotlinPsiManager.getEclispeFile(navigationFile);
	    } else {
	        kotlinFile = NavigationPackage.chooseSourceFile(sourceFiles);
	    }
	    
	    try {
	        if (kotlinFile != null && kotlinFile.exists()) {
	            return EditorUtility.openInEditor(kotlinFile, activate);
	        }
	    } catch (PartInitException e) {
	        KotlinLogger.logAndThrow(e);
	    }
	    
	    return null;
	}
	
	public static void revealKotlinElement(@NotNull KotlinEditor kotlinEditor, @NotNull IJavaElement javaElement) {
        JetFile jetFile = kotlinEditor.getParsedFile();
        
        if (jetFile == null) {
            return;
        }
        
        JetElement jetElement = NavigationPackage.findKotlinDeclaration(javaElement, jetFile);
        if (jetElement == null) {
            jetElement = jetFile;
        }
        
        int offset = LineEndUtil.convertLfToDocumentOffset(jetFile.getText(), jetElement.getTextOffset(), kotlinEditor.getDocument());
        kotlinEditor.getJavaEditor().selectAndReveal(offset, 0);
	}
	
	@Nullable
	public static IEditorPart openKotlinClassFileEditor(@NotNull IJavaElement element, boolean activate) {
        if (element instanceof ClassFile) {
            ClassFile classFile = (ClassFile) element;
            
            KotlinClassFileEditorInput editorInput = new KotlinClassFileEditorInput(classFile,
                    classFile.getJavaProject());
            
            IWorkbench wb = PlatformUI.getWorkbench();
            IWorkbenchWindow win = wb.getActiveWorkbenchWindow();
            IWorkbenchPage page = win.getActivePage();
            
            try {
                IEditorPart reusedEditor = page.openEditor(editorInput, KotlinClassFileEditor.EDITOR_ID, activate);
                if (reusedEditor != null) {
                    //the input is compared by a source path, but corresponding classes may be different
                    //so if editor is reused, the input should be changed for the purpose of inner navigation
                    page.reuseEditor((IReusableEditor) reusedEditor, editorInput);
                }
                return reusedEditor;
            } catch (PartInitException e) {
                KotlinLogger.logAndThrow(e);
            }
        }        
        return null;
	}
}
