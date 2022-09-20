package org.jetbrains.kotlin.ui.navigation;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.internal.core.BinaryType;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.filesystem.KotlinLightClassManager;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil;
import org.jetbrains.kotlin.psi.KtElement;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.ui.editors.KotlinClassFileEditor;
import org.jetbrains.kotlin.ui.editors.KotlinClassFileEditorInput;
import org.jetbrains.kotlin.ui.editors.KotlinEditor;

// Seeks Kotlin editor by IJavaElement
@SuppressWarnings("restriction")
public class KotlinOpenEditor {
    private static final String CLASS_WITHOUT_SOURCE = "*.class without source";

	@Nullable
    public static IEditorPart openKotlinEditor(@NotNull IJavaElement element, boolean activate) {
        List<KtFile> sourceFiles = findSourceFiles(element);
        
        IFile kotlinFile;
        if (sourceFiles.size() == 1) {
            kotlinFile = KotlinPsiManager.getEclipseFile(sourceFiles.get(0));
        } else {
            kotlinFile = KotlinOpenEditorUtilsKt.chooseSourceFile(sourceFiles);
        }
        
        try {
            if (kotlinFile != null && kotlinFile.exists()) {
                IEditorPart kotlinEditor = EditorUtility.openInEditor(kotlinFile, activate);
                if (activate && kotlinEditor instanceof KotlinEditor) {
                    revealKotlinElement((KotlinEditor) kotlinEditor, element);
                }
                
                return kotlinEditor;
            }
        } catch (PartInitException e) {
            KotlinLogger.logAndThrow(e);
        }
        
        return null;
    }
    
    @NotNull
    public static List<KtFile> findSourceFiles(@NotNull IJavaElement element) {
        IResource resource = element.getResource();
        if (resource == null) {
            return Collections.emptyList();
        }
        
        File lightClass = resource.getFullPath().toFile();
        List<KtFile> sourceFiles = KotlinLightClassManager.Companion
                .getInstance(element.getJavaProject().getProject()).getSourceFiles(lightClass);
        KtFile navigationFile = KotlinOpenEditorUtilsKt.findNavigationFileFromSources(element, sourceFiles);
        
        if (navigationFile != null) {
            return Collections.singletonList(navigationFile);
        } else {
            return sourceFiles;
        }
    }
    
    public static void revealKotlinElement(@NotNull KotlinEditor kotlinEditor, @NotNull IJavaElement javaElement) {
        KtFile jetFile = kotlinEditor.getParsedFile();
        
        if (jetFile == null) {
            return;
        }
        
        KtElement jetElement = KotlinOpenEditorUtilsKt.findKotlinDeclaration(javaElement, jetFile);
        if (jetElement == null) {
            jetElement = jetFile;
        }
        
        int offset = LineEndUtil.convertLfToDocumentOffset(jetFile.getText(), jetElement.getTextOffset(),
                kotlinEditor.getDocument());
        kotlinEditor.getJavaEditor().selectAndReveal(offset, 0);
    }
    
    @Nullable
    public static IEditorPart openKotlinClassFileEditor(@NotNull IJavaElement element, boolean activate) {
        IClassFile classFile;
        if (element instanceof IClassFile) {
            classFile = (IClassFile) element;
        } else if (element instanceof BinaryType) {
            classFile = ((BinaryType) element).getClassFile();
        } else {
            return null;
        }
        
        KotlinClassFileEditorInput editorInput = new KotlinClassFileEditorInput(classFile, classFile.getJavaProject());
        
        IWorkbench wb = PlatformUI.getWorkbench();
        IWorkbenchWindow win = wb.getActiveWorkbenchWindow();
        IWorkbenchPage page = win.getActivePage();
        
        try {
            IEditorPart reusedEditor = page.openEditor(editorInput, resolveEditorID((ISourceReference)element), activate);
            if (reusedEditor != null) {
                // the input is compared by a source path, but corresponding
                // classes may be different
                // so if editor is reused, the input should be changed for the
                // purpose of inner navigation
                page.reuseEditor((IReusableEditor) reusedEditor, editorInput);
            }
            return reusedEditor;
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
        return null;
    }
    
	private static String resolveEditorID(@NotNull ISourceReference reference) throws CoreException {
		// if no source let the java decompiler handle it.
		if(reference.getSourceRange() != null && reference.getSourceRange().getLength() > 0) {
    		return KotlinClassFileEditor.Companion.getEDITOR_ID();
		}
		return IDE.getEditorDescriptor(CLASS_WITHOUT_SOURCE, true, false).getId();
    }
}
