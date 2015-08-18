/*******************************************************************************
 * Copyright 2000-2015 JetBrains s.r.o.
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
package org.jetbrains.kotlin.ui.editors;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.jdt.ui.actions.SelectionDispatchAction;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.util.OpenStrategy;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.references.KotlinReference;
import org.jetbrains.kotlin.core.references.ReferencesPackage;
import org.jetbrains.kotlin.core.resolve.lang.java.resolver.EclipseJavaSourceElement;
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaElement;
import org.jetbrains.kotlin.descriptors.SourceElement;
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.psi.JetReferenceExpression;
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;

public class KotlinOpenDeclarationAction extends SelectionDispatchAction {
    
    public static final String OPEN_EDITOR_TEXT = "OpenEditor";
    
    @NotNull
    private final KotlinEditor editor;
    
    public KotlinOpenDeclarationAction(@NotNull KotlinEditor editor) {
        super(editor.getJavaEditor().getSite());
        this.editor = editor;
        
        setText(ActionMessages.OpenAction_declaration_label);
        setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_EDITOR);
    }
    
    @Override
    public void run(ITextSelection selection) {
        JetFile file = editor.getParsedFile();
        
        if (file == null) {
            return;
        }
        
        IJavaProject javaProject = editor.getJavaProject();
        
        if (javaProject == null) {
            return;
        }
        
        SourceElement element = getTargetElement(getSelectedExpressionWithParsedFile(editor, file, selection.getOffset()), file, javaProject);
        if (element == null) {
            return;
        }
        
        try {
            gotoElement(element, javaProject);
        } catch (JavaModelException e) {
            KotlinLogger.logError(e);
        } catch (PartInitException e) {
            KotlinLogger.logError(e);
        }
    }
    
    @Nullable
    private SourceElement getTargetElement(@Nullable JetReferenceExpression expression, @NotNull JetFile file, @NotNull IJavaProject javaProject) {
        if (expression == null) {
            return null;
        }
        KotlinReference reference = ReferencesPackage.createReference(expression);
        List<SourceElement> sourceElements = ReferencesPackage.resolveToSourceElements(reference);
        
        return sourceElements.size() == 1 ? sourceElements.get(0) : null; 
    }
    
    private void gotoElement(@NotNull SourceElement element, @NotNull IJavaProject javaProject) throws JavaModelException, PartInitException {
        if (element instanceof EclipseJavaSourceElement) {
            IBinding binding = ((EclipseJavaElement<?>) ((EclipseJavaSourceElement) element).getJavaElement()).getBinding();
            gotoJavaDeclaration(binding, javaProject);
        } else if (element instanceof KotlinSourceElement) {
            PsiElement psiElement = ((KotlinSourceElement) element).getPsi();
            gotoKotlinDeclaration(psiElement, javaProject);
        }
    }
    
    private void gotoKotlinDeclaration(@NotNull PsiElement element, @NotNull IJavaProject javaProject) throws PartInitException, JavaModelException {
        VirtualFile virtualFile = element.getContainingFile().getVirtualFile();
        assert virtualFile != null;
        
        IFile targetFile = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(virtualFile.getPath()));
        if (targetFile == null) {
            targetFile = EditorsPackage.getAcrhivedFileFromVirtual(virtualFile);
        }
        IEditorPart editorPart = findEditorPart(targetFile, element, javaProject);
        if (editorPart == null) {
            return;
        }
        
        AbstractTextEditor targetEditor = (AbstractTextEditor) editorPart;
        
        int start = LineEndUtil.convertLfToDocumentOffset(element.getContainingFile().getText(), 
                element.getTextOffset(), editor.getDocument());
        targetEditor.selectAndReveal(start, 0);
    }
    
    private static void gotoJavaDeclaration(@NotNull IBinding binding, @NotNull IJavaProject javaProject) throws PartInitException, JavaModelException {
        IJavaElement javaElement = binding.getJavaElement();
        if (javaElement == null && binding instanceof IMethodBinding) { 
            IMethodBinding methodBinding = (IMethodBinding) binding;
            if (methodBinding.isConstructor()) { // because <init>() may correspond to null java element
                javaElement = methodBinding.getDeclaringClass().getJavaElement();
            }
        } 
        
        if (javaElement != null) {
            IEditorPart editorPart = EditorUtility.openInEditor(javaElement, OpenStrategy.activateOnOpen());
            JavaUI.revealInEditor(editorPart, javaElement);
        }
    }
    
    @Nullable
    private static IEditorPart findEditorPart(@Nullable IFile targetFile, @NotNull PsiElement element, 
            @NotNull IJavaProject javaProject) throws JavaModelException, PartInitException {
        if (targetFile != null && targetFile.exists()) {
            return openInEditor(targetFile);
        }
        
        PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
        if (psiClass != null) {
            IType targetType = javaProject.findType(psiClass.getQualifiedName());
            return EditorUtility.openInEditor(targetType, true);
        }

        //external jar
        if (targetFile != null && targetFile.getFullPath().toOSString().contains("jar")) {
            PsiFile elementFile = element.getContainingFile();
            
            if (elementFile == null) {
                return null;
            }
            
            IWorkbench wb = PlatformUI.getWorkbench();
            IWorkbenchWindow win = wb.getActiveWorkbenchWindow();

            String fileText = elementFile.getText();
            String fileName = elementFile.getName();

            PsiDirectory containingDirectory = elementFile.getContainingDirectory();
            StringStorage storage = new StringStorage(fileText, fileName, EditorsPackage.getFqNameInsideArchive(containingDirectory.toString()));
            IStorageEditorInput input = new StringInput(storage);
            IWorkbenchPage page = win.getActivePage();
            if (page != null) {
                IEditorPart reusedEditor = page.findEditor(input);
                if (reusedEditor != null) {
                    page.reuseEditor((IReusableEditor) reusedEditor, input);
                }
                return page.openEditor(input, "org.jetbrains.kotlin.ui.editors.KotlinFileEditor");
            }
        }
        
        return null;
    }
    
    @Nullable
    public static JetReferenceExpression getSelectedExpression(@NotNull KotlinEditor editor, @NotNull IFile file, int offset) {
        return getSelectedExpressionWithParsedFile(editor, KotlinPsiManager.INSTANCE.getParsedFile(file), offset);
    }
    
    @Nullable
    public static JetReferenceExpression getSelectedExpressionWithParsedFile(@NotNull KotlinEditor editor, @NotNull JetFile file, int offset) {
        offset = LineEndUtil.convertCrToDocumentOffset(editor.getJavaEditor().getViewer().getDocument(), offset);
        
        PsiElement psiExpression = file.findElementAt(offset);
        if (psiExpression == null) {
            return null;
        }
        
        return ReferencesPackage.getReferenceExpression(psiExpression);
    }
    
    @Nullable
    public static IEditorPart openInEditor(IFile file) {
        try {
            IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
            return IDE.openEditor(page, file, false);
        } catch (PartInitException e) {
            KotlinLogger.logAndThrow(e);
        }
        
        return null;
    }
}