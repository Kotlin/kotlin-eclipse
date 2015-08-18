package org.jetbrains.kotlin.ui.navigation;

import java.io.File;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.core.filesystem.KotlinLightClassManager;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil;
import org.jetbrains.kotlin.psi.JetElement;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.ui.editors.KotlinEditor;

// Seeks Kotlin editor by IJavaElement
public class KotlinOpenEditor {
	@Nullable
	public static IEditorPart openKotlinEditor(@NotNull IJavaElement element, boolean activate) {
	    File lightClass = element.getResource().getFullPath().toFile();
	    List<JetFile> sourceFiles = KotlinLightClassManager.getInstance(element.getJavaProject()).getSourceFiles(lightClass);
	    JetFile referenceFile = null;
	    for (JetFile sourceFile : sourceFiles) {
	        JetElement referenceElement = NavigationPackage.findKotlinDeclaration(element, sourceFile);
	        if (referenceElement != null) {
	            referenceFile = sourceFile;
	            break;
	        }
	    }
	    
	    if (referenceFile == null) {
	        return null;
	    }
	    
	    IPath sourceFilePath = new Path(referenceFile.getVirtualFile().getPath());
	    IFile kotlinFile = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(sourceFilePath);
	    
	    try {
	        if (kotlinFile.exists()) {
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
}
