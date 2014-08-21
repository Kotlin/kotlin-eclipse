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
package org.jetbrains.kotlin.ui.editors;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.jdt.ui.actions.SelectionDispatchAction;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.util.OpenStrategy;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.SourceElement;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.source.KotlinSourceElement;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.resolve.EclipseDescriptorUtils;
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer;
import org.jetbrains.kotlin.core.resolve.lang.java.resolver.EclipseJavaSourceElement;
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaElement;
import org.jetbrains.kotlin.utils.EditorUtil;
import org.jetbrains.kotlin.utils.LineEndUtil;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;

public class KotlinOpenDeclarationAction extends SelectionDispatchAction {
    
    public static final String OPEN_EDITOR_TEXT = "OpenEditor";
    
    @NotNull
    private final KotlinEditor editor;
    private final IJavaProject javaProject;
    private final IFile file;
    
    public KotlinOpenDeclarationAction(@NotNull KotlinEditor editor) {
        super(editor.getSite());
        this.editor = editor;
        file = EditorUtil.getFile(editor);
        javaProject = JavaCore.create(file.getProject());
        
        setText(ActionMessages.OpenAction_declaration_label);
        setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_EDITOR);
    }
    
    @Override
    public void run(ITextSelection selection) {
        SourceElement element = getTargetElement(getSelectedExpression(file, selection.getOffset()));
        
        if (element == null) {
            return;
        }
        
        try {
            gotoElement(element);
        } catch (JavaModelException e) {
            KotlinLogger.logError(e);
        } catch (PartInitException e) {
            KotlinLogger.logError(e);
        }
    }
    
    @Nullable
    private SourceElement getTargetElement(@Nullable JetReferenceExpression expression) {
        BindingContext bindingContext = KotlinAnalyzer
                .analyzeOneFileCompletely(javaProject, KotlinPsiManager.INSTANCE.getParsedFile(file))
                .getBindingContext();
        DeclarationDescriptor descriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, expression);
        if (descriptor != null) {
            List<SourceElement> declarations = EclipseDescriptorUtils.descriptorToDeclarations(descriptor);
            
            if (declarations.size() > 1 || declarations.isEmpty()) {
                return null;
            }
            
            return declarations.get(0);
        }
        
        return null;
    }
    
    private void gotoElement(@NotNull SourceElement element) throws JavaModelException, PartInitException {
        if (element instanceof EclipseJavaSourceElement) {
            IBinding binding = ((EclipseJavaElement<?>) ((EclipseJavaSourceElement) element).getJavaElement()).getBinding();
            gotoJavaDeclaration(binding, javaProject);
        } else if (element instanceof KotlinSourceElement) {
            PsiElement psiElement = ((KotlinSourceElement) element).getPsi();
            gotoKotlinDeclaration(psiElement, javaProject);
        }
    }
    
    private static void gotoKotlinDeclaration(@NotNull PsiElement element, @NotNull IJavaProject javaProject) throws PartInitException, JavaModelException {
        VirtualFile virtualFile = element.getContainingFile().getVirtualFile();
        assert virtualFile != null;
        
        IFile targetFile = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(virtualFile.getPath()));
        IEditorPart editorPart = findEditorPart(targetFile, element, javaProject);
        if (editorPart == null) {
            return;
        }
        
        AbstractTextEditor targetEditor = (AbstractTextEditor) editorPart;
        
        int start = LineEndUtil.convertLfToOsOffset(element.getContainingFile().getText(), element.getTextOffset());
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
        if (psiClass == null) {
            return null;
        }
        
        IType targetType = javaProject.findType(psiClass.getQualifiedName());
        
        return EditorUtility.openInEditor(targetType, true);
    }
    
    @Nullable
    private JetReferenceExpression getSelectedExpression(@NotNull IFile file, int offset) {
        return getSelectedExpression(editor, file, offset);
    }
    
    @Nullable
    public static JetReferenceExpression getSelectedExpression(@NotNull JavaEditor editor, @NotNull IFile file, int offset) {
        offset = LineEndUtil.convertCrToOsOffset(editor.getViewer().getDocument().get(), offset);
        
        PsiElement psiExpression = ((JetFile) KotlinPsiManager.INSTANCE.getParsedFile(file)).findElementAt(offset);
        if (psiExpression == null) {
            return null;
        }
        
        return PsiTreeUtil.getParentOfType(psiExpression, JetSimpleNameExpression.class);
    }
    
    @Nullable
    public static IEditorPart openInEditor(IFile file) throws PartInitException {
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        return IDE.openEditor(page, file, false);
    }
}