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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.actions.SelectionDispatchAction;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorToSourceUtils;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer;
import org.jetbrains.kotlin.utils.EditorUtil;
import org.jetbrains.kotlin.utils.LineEndUtil;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTreeUtil;

public class OpenDeclarationAction extends SelectionDispatchAction {

    private final JavaEditor editor;
    private final IJavaProject javaProject;
    private final IFile file;
    
    public OpenDeclarationAction(JavaEditor editor) {
        super(editor.getSite());
        this.editor = editor;
        file = EditorUtil.getFile(editor);
        javaProject = JavaCore.create(file.getProject());
    }

    @Override
    public void run(ITextSelection selection) {
        JetFile jetFile = (JetFile) KotlinPsiManager.INSTANCE.getParsedFile(file);
        JetReferenceExpression expression = getSelectedExpression(jetFile, selection.getOffset());
        PsiElement element = getTargetElement(expression);
        
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
    private PsiElement getTargetElement(@Nullable JetReferenceExpression expression) {
        BindingContext bindingContext = KotlinAnalyzer.analyzeOnlyOneFileCompletely(javaProject, KotlinPsiManager.INSTANCE.getParsedFile(file));
        DeclarationDescriptor descriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, expression);
        
        if (descriptor == null) return null;
        
        List<PsiElement> elements = DescriptorToSourceUtils.descriptorToDeclarations(descriptor);
        if (elements.size() > 1 || elements.isEmpty()) {
            return null;
        }
        
        return elements.get(0);
    }
    
    private void gotoElement(@NotNull PsiElement element) throws JavaModelException, PartInitException {
        VirtualFile virtualFile = element.getContainingFile().getVirtualFile();
        assert virtualFile != null;
        
        IFile targetFile = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(virtualFile.getPath()));
        IEditorPart editorPart = findEditorPart(targetFile, element);
        if (editorPart == null) {
            return;
        }
        
        AbstractTextEditor targetEditor = (AbstractTextEditor) editorPart;
        
        if (targetEditor instanceof KotlinEditor) {
            int start = LineEndUtil.convertLfToOsOffset(element.getContainingFile().getText(), element.getTextOffset());
            targetEditor.selectAndReveal(start, 0);
        } else if (targetEditor instanceof JavaEditor) {
            gotoDeclarationInJavaFile(element, (JavaEditor) targetEditor);
        }
    }
    
    @Nullable
    private IEditorPart findEditorPart(@Nullable IFile targetFile, @NotNull PsiElement element) throws JavaModelException, PartInitException {
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
    
    private void gotoDeclarationInJavaFile(@NotNull PsiElement element, @NotNull JavaEditor editor) throws PartInitException, JavaModelException {
        if (element instanceof PsiClass) {
            IJavaElement targetJavaElement = getJavaClass(((PsiClass) element).getQualifiedName(), editor);
            JavaUI.revealInEditor(editor, targetJavaElement);
        } else if (element instanceof PsiMethod) {
            List<IMethod> methods = getJavaMethods((PsiMethod) element, editor);
            
            if (methods.size() == 1) {
                JavaUI.revealInEditor(editor, (IJavaElement) methods.get(0));
            } else {
                IJavaElement targetJavaElement = selectMethod(methods.toArray(new IMethod[methods.size()]));
                JavaUI.revealInEditor(editor, targetJavaElement);
            }
        }
    }
    
    private boolean matchMethodSignature(IMethod javaMethod, PsiMethod psiMethod) {
        if (!javaMethod.getElementName().equals(psiMethod.getName())) {
            return false;
        }
        
        try {
            ILocalVariable[] javaParameterTypes = javaMethod.getParameters();
            PsiType[] psiParameterTypes = psiMethod.getHierarchicalMethodSignature().getParameterTypes();
            
            if (javaParameterTypes.length != psiParameterTypes.length) {
                return false;
            }
            
            for (int i = 0; i < javaParameterTypes.length; ++i) {
                String psiCanonicalName = psiParameterTypes[i].getCanonicalText();
                String javaCanonicalName = Signature.toString(javaParameterTypes[i].getTypeSignature());
                if (!psiCanonicalName.equals(javaCanonicalName)) {
                    return false;
                }
            }
        } catch (JavaModelException e) {
            KotlinLogger.logError(e);
        }  
        
        return true;
    }
    
    @NotNull
    private List<IMethod> getJavaMethods(@NotNull PsiMethod psiMethod, @NotNull JavaEditor editor) throws JavaModelException {
        PsiClass psiClass = psiMethod.getContainingClass();
        assert psiClass != null;
        
        IType javaClass = getJavaClass(psiClass.getQualifiedName(), editor);
        if (javaClass == null) {
            return Collections.emptyList();
        }
        
        List<IMethod> methods = new ArrayList<IMethod>();
        for (IMethod javaMethod : javaClass.getMethods()) {
            if (matchMethodSignature(javaMethod, psiMethod)) {
                methods.add(javaMethod);
            }
        }
        
        return methods;
    }
    
    @Nullable
    private IType getJavaClass(@Nullable String qualifiedName, @NotNull JavaEditor javaEditor) throws JavaModelException {
        List<IType> types = new ArrayList<IType>();
        
        Object viewPartInput = javaEditor.getViewPartInput();
        if (viewPartInput instanceof ICompilationUnit) {
            IType[] typesInEditor = ((ICompilationUnit) viewPartInput).getAllTypes(); 
            for (IType type : typesInEditor) {
                types.add(type);
            }
        } else if (viewPartInput instanceof IClassFile) {
            types.addAll(getTypes((IClassFile) viewPartInput));
        } else {
            return null;
        }
        
        for (IType type : types) {
            String javaQualifiedName = type.getFullyQualifiedName('.');
            if (javaQualifiedName.equals(qualifiedName)) {
                return type;
            }
        }
        
        return null;
    }
    
    private List<IType> getTypes(@NotNull IClassFile classFile) throws JavaModelException {
        IJavaElement[] javaElements = classFile.getChildren();
        
        List<IType> types = new ArrayList<IType>();
        for (IJavaElement javaElement : javaElements) {
            if (javaElement instanceof IType) {
                IType topLevelType = (IType) javaElement;
                
                types.add(topLevelType);
                types.addAll(getNestedTypes(topLevelType));
            }
        } 
        
        return types;
    }
    
    private List<IType> getNestedTypes(@NotNull IType javaType) throws JavaModelException {
        List<IType> nestedTypes = new ArrayList<IType>();
        for (IType nestedType : javaType.getTypes()) {
            nestedTypes.add(nestedType);
        }
        
        return nestedTypes;
    }
    
    @Nullable
    private IJavaElement selectMethod(IMethod[] methods) {
        int flags = JavaElementLabelProvider.SHOW_DEFAULT | JavaElementLabelProvider.SHOW_QUALIFIED | JavaElementLabelProvider.SHOW_ROOT;

        ElementListSelectionDialog dialog = new ElementListSelectionDialog(getShell(), new JavaElementLabelProvider(flags));
        dialog.setTitle("Method navigation");
        dialog.setMessage("Select method to navigate");
        dialog.setElements(methods);

        if (dialog.open() == Window.OK) {
            return (IJavaElement) dialog.getFirstResult();
        }
        
        return null;
    }
    
    @Nullable
    private JetReferenceExpression getSelectedExpression(@NotNull JetFile jetFile, int offset) {
        IDocument document = editor.getViewer().getDocument();
        
        offset = LineEndUtil.convertCrToOsOffset(document.get(), offset);
        
        PsiElement psiExpression = jetFile.findElementAt(offset);
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
    
    @Override
    public void run(IStructuredSelection selection) {
    }
}