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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
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
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.references.KotlinReference;
import org.jetbrains.kotlin.core.references.KotlinReferenceKt;
import org.jetbrains.kotlin.core.references.ReferenceUtilsKt;
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer;
import org.jetbrains.kotlin.core.resolve.lang.java.resolver.EclipseJavaSourceElement;
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaElement;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.SourceElement;
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil;
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass;
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryPackageSourceElement;
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement;
import org.jetbrains.kotlin.load.kotlin.VirtualFileKotlinClass;
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader;
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader.Kind;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtReferenceExpression;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement;
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedCallableMemberDescriptor;
import org.jetbrains.kotlin.ui.navigation.KotlinOpenEditor;

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
        KtReferenceExpression selectedExpression = getSelectedExpressionWithParsedFile(editor, selection.getOffset());
        if (selectedExpression == null) {
            return;
        }
        
        List<KotlinReference> reference = KotlinReferenceKt.createReferences(selectedExpression);
        SourceElement element = getTargetElement(reference);
        if (element == null) {
            return;
        }
        
        try {
            IJavaProject javaProject = editor.getJavaProject();
            if (javaProject == null) {
                return;
            }
            
            gotoElement(element, reference.get(0), javaProject);
        } catch (JavaModelException e) {
            KotlinLogger.logError(e);
        } catch (PartInitException e) {
            KotlinLogger.logError(e);
        }
    }
    
    @Nullable
    private SourceElement getTargetElement(@NotNull List<KotlinReference> reference) {
        List<SourceElement> sourceElements = ReferenceUtilsKt.resolveToSourceElements(reference);
        for (SourceElement sourceElement : sourceElements) {
            if (!sourceElement.equals(SourceElement.NO_SOURCE)) return sourceElement;
        }
        return null; 
    }
    
    private void gotoElement(@NotNull SourceElement element, KotlinReference kotlinReference, @NotNull IJavaProject javaProject) throws JavaModelException, PartInitException {
        if (element instanceof EclipseJavaSourceElement) {
            IBinding binding = ((EclipseJavaElement<?>) ((EclipseJavaSourceElement) element).getJavaElement()).getBinding();
            gotoJavaDeclaration(binding, javaProject);
        } else if (element instanceof KotlinSourceElement) {
            PsiElement psiElement = ((KotlinSourceElement) element).getPsi();
            gotoKotlinDeclaration(psiElement, kotlinReference, javaProject);
        } else if (element instanceof KotlinJvmBinarySourceElement) {
            KotlinJvmBinaryClass binaryClass = ((KotlinJvmBinarySourceElement) element).getBinaryClass();
            DeclarationDescriptor descriptor = getDeclarationDescriptor(kotlinReference, javaProject);
            KtFile kotlinFile = kotlinReference.getExpression().getContainingKtFile();
            gotoElementInBinaryClass(binaryClass, descriptor, kotlinFile, javaProject);
        } else if (element instanceof KotlinJvmBinaryPackageSourceElement) {
            KotlinJvmBinaryPackageSourceElement binaryElement = (KotlinJvmBinaryPackageSourceElement) element;
            gotoClassByPackageSourceElement(binaryElement, kotlinReference, javaProject);
        }
    }
    
    private void gotoClassByPackageSourceElement(KotlinJvmBinaryPackageSourceElement sourceElement, KotlinReference kotlinReference, IJavaProject javaProject) throws PartInitException, JavaModelException {
        DeclarationDescriptor descriptor = getDeclarationDescriptor(kotlinReference, javaProject);
        if (descriptor instanceof DeserializedCallableMemberDescriptor) {
            KotlinJvmBinaryClass binaryClass = sourceElement.getContainingBinaryClass((DeserializedCallableMemberDescriptor) descriptor);
            KtFile kotlinFile = kotlinReference.getExpression().getContainingKtFile();
            gotoElementInBinaryClass(binaryClass, descriptor, kotlinFile, javaProject);
        }
    }
    
    private void gotoElementInBinaryClass(KotlinJvmBinaryClass binaryClass, DeclarationDescriptor descriptor, 
            KtFile kotlinFile, IJavaProject javaProject) throws JavaModelException, PartInitException {
        IClassFile classFile = findImplementingClass(binaryClass, descriptor, javaProject);
        if (classFile == null) {
            return;
        }
        
        KotlinClassFileEditor targetEditor = (KotlinClassFileEditor) KotlinOpenEditor.openKotlinClassFileEditor(classFile, OpenStrategy.activateOnOpen());
        
        if (targetEditor == null) {
            return;
        }
        
        
        KtFile targetKtFile = targetEditor.getParsedFile();
        int offset = KotlinSearchDeclarationVisitorKt.findDeclarationInParsedFile(descriptor, targetKtFile);
        int start = LineEndUtil.convertLfToDocumentOffset(targetKtFile.getText(), offset, targetEditor.getDocument());
        targetEditor.selectAndReveal(start, 0);
    }
    
    @Nullable
    private DeclarationDescriptor getDeclarationDescriptor(KotlinReference kotlinReference, IJavaProject javaProject) {
        KtFile jetFile = kotlinReference.getExpression().getContainingKtFile();
        BindingContext context = KotlinAnalyzer.analyzeFile(javaProject, jetFile).getAnalysisResult().getBindingContext();
        Collection<DeclarationDescriptor> descriptors = kotlinReference.getTargetDescriptors(context);
        if (descriptors.isEmpty()) {
            return null;
        }
        
        //TODO: popup if there's several descriptors to navigate to
        return descriptors.iterator().next();
    }

    private IClassFile findImplementingClass(KotlinJvmBinaryClass binaryClass, DeclarationDescriptor descriptor, IJavaProject javaProject)
            throws JavaModelException {
        Kind binaryClassKind = binaryClass.getClassHeader().getKind();
        if (KotlinClassHeader.Kind.MULTIFILE_CLASS.equals(binaryClassKind) || 
                KotlinClassHeader.Kind.PACKAGE_FACADE.equals(binaryClassKind)) {
            return getImplementingFacadePart(binaryClass, descriptor, javaProject);
        }
        
        return getClassFile(binaryClass, javaProject);
    }

    private IClassFile getImplementingFacadePart(KotlinJvmBinaryClass binaryClass, DeclarationDescriptor descriptor,
            IJavaProject javaProject) throws JavaModelException {
        if (descriptor instanceof DeserializedCallableMemberDescriptor) {
            DeserializedCallableMemberDescriptor memberDescriptor 
                = (DeserializedCallableMemberDescriptor) descriptor;
            Name className = getImplClassName(memberDescriptor);
            IPackageFragment fragment = (IPackageFragment) getClassFile(binaryClass, javaProject).getParent();
            IClassFile file = fragment.getClassFile(className.asString() + ".class");
            if (file.exists()) {
                return file;
            }
        }
        return null;
    }

    //implemented via Reflection because Eclipse has issues with ProGuard-shrunken compiler 
    //in com.google.protobuf.GeneratedMessageLite.ExtendableMessage
    private Name getImplClassName(DeserializedCallableMemberDescriptor memberDescriptor) {
        int nameIndex;
        try {
            Method getProtoMethod = DeserializedCallableMemberDescriptor.class.getMethod("getProto");
            Object proto = getProtoMethod.invoke(memberDescriptor);
            
            Field implClassNameField = Class.forName("org.jetbrains.kotlin.serialization.jvm.JvmProtoBuf").getField("implClassName");
            Object implClassName = implClassNameField.get(null);
            
            
            Class<?> protobufCallable = Class.forName("org.jetbrains.kotlin.serialization.ProtoBuf$Callable");
            Method getExtensionMethod = protobufCallable.getMethod("getExtension", implClassName.getClass());
            Object indexObj = getExtensionMethod.invoke(proto, implClassName);
            
            if (!(indexObj instanceof Integer)) {
                return null;
            }
            nameIndex = ((Integer)indexObj).intValue();
        } catch (ReflectiveOperationException | IllegalArgumentException | SecurityException e) {
            return null;
        }
        
        Name className = memberDescriptor.getNameResolver().getName(nameIndex);
        return className;
    }

    private IClassFile getClassFile(KotlinJvmBinaryClass binaryClass, IJavaProject javaProject)
            throws JavaModelException {
        VirtualFile file = ((VirtualFileKotlinClass)binaryClass).getFile();
        
        String packagePath = file.getParent().getPath();
        IPackageFragment fragment = javaProject.findPackageFragment(JarNavigationUtilsKt.pathFromUrlInArchive(packagePath));
        
        String className = file.getName();
        IClassFile classFile = fragment.getClassFile(className);
        return classFile;
    }
    
    private void gotoKotlinDeclaration(@NotNull PsiElement element, KotlinReference kotlinReference, @NotNull IJavaProject javaProject) throws PartInitException, JavaModelException {
        AbstractTextEditor targetEditor = findEditorForReferencedElement(element, kotlinReference, javaProject);
        if (targetEditor == null) return;
        
        if (targetEditor instanceof KotlinEditor) {
            KotlinEditor kotlinEditor = (KotlinEditor) targetEditor;
            int start = LineEndUtil.convertLfToDocumentOffset(element.getContainingFile().getText(), 
                    element.getTextOffset(), kotlinEditor.getDocument());
            targetEditor.selectAndReveal(start, 0);
        }
    }
    
    private AbstractTextEditor findEditorForReferencedElement(@NotNull PsiElement element,
            KotlinReference kotlinReference, @NotNull IJavaProject javaProject) throws PartInitException,
            JavaModelException {
        // if element is in the same file
        if (kotlinReference.getExpression().getContainingFile().equals(element.getContainingFile())) {
            return editor.getJavaEditor();
        }
        VirtualFile virtualFile = element.getContainingFile().getVirtualFile();
        if (virtualFile == null) {
            return null;
        }
        
        IFile targetFile = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(virtualFile.getPath()));
        if (targetFile == null) {
            targetFile = JarNavigationUtilsKt.getAcrhivedFileFromPath(virtualFile.getPath());
        }
        IEditorPart editorPart = findEditorPart(targetFile, element, javaProject);
        if (!(editorPart instanceof AbstractTextEditor)) {
            return null;
        }
        return (AbstractTextEditor) editorPart;
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
            StringStorage storage = new StringStorage(fileText, fileName, JarNavigationUtilsKt.getFqNameInsideArchive(containingDirectory.toString()));
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
    public static KtReferenceExpression getSelectedExpression(@NotNull KotlinEditor editor, @NotNull IFile file, int offset) {
        return getSelectedExpressionWithParsedFile(editor, offset);
    }
    
    @Nullable
    public static KtReferenceExpression getSelectedExpressionWithParsedFile(@NotNull KotlinEditor editor, int offset) {
        KtFile file = editor.getParsedFile();
        if (file == null) return null;
        
        offset = LineEndUtil.convertCrToDocumentOffset(editor.getDocument(), offset);
        
        PsiElement psiExpression = file.findElementAt(offset);
        if (psiExpression == null) return null;
        
        return ReferenceUtilsKt.getReferenceExpression(psiExpression);
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