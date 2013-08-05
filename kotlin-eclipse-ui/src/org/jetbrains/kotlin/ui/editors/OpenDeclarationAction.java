package org.jetbrains.kotlin.ui.editors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.actions.SelectionDispatchAction;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer;
import org.jetbrains.kotlin.core.utils.KotlinEnvironment;
import org.jetbrains.kotlin.utils.LineEndUtil;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;

public class OpenDeclarationAction extends SelectionDispatchAction {

    private final JavaEditor editor;
    private final IJavaProject javaProject;
    private final IFile file;
    
    public OpenDeclarationAction(JavaEditor editor) {
        super(editor.getSite());
        this.editor = editor;
        file = (IFile) editor.getEditorInput().getAdapter(IFile.class);
        javaProject = JavaCore.create(file.getProject());
    }

    @Override
    public void run(ITextSelection selection) {
        KotlinEnvironment kotlinEnvironment = new KotlinEnvironment(javaProject);
        
        JetFile jetFile = kotlinEnvironment.getJetFile(file);
        assert jetFile != null;
        
        JetReferenceExpression expression = getSelectedExpression(jetFile, selection.getOffset());
        PsiElement element = getTargetElement(kotlinEnvironment, expression);
        
        if (element == null) {
            return;
        }
        
        try {
            gotoElement(element, expression);
        } catch (JavaModelException e) {
            KotlinLogger.logError(e);
        } catch (PartInitException e) {
            KotlinLogger.logError(e);
        }
    }
    
    private PsiElement getTargetElement(KotlinEnvironment kotlinEnvironment, JetReferenceExpression expression) {
        BindingContext bindingContext = KotlinAnalyzer.analyzeProject(javaProject, kotlinEnvironment);
        List<PsiElement> elements = BindingContextUtils.resolveToDeclarationPsiElements(bindingContext, expression);
        if (elements.size() > 1 || elements.isEmpty()) {
            return null;
        }
        
        return elements.get(0);
    }
    
    private void gotoElement(@NotNull PsiElement element, JetReferenceExpression expr) throws JavaModelException, PartInitException {
        VirtualFile virtualFile = element.getContainingFile().getVirtualFile();
        assert virtualFile != null;
        
        IFile targetFile = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(virtualFile.getCanonicalPath()));
        AbstractTextEditor targetEditor = (AbstractTextEditor) openInEditor(targetFile);
        if (targetEditor instanceof KotlinEditor) {
            int start = LineEndUtil.convertLfToOsOffset(element.getContainingFile().getText(), element.getTextOffset());
            targetEditor.selectAndReveal(start, 0);
        } else if (targetEditor instanceof JavaEditor) {
            gotoDeclarationInJavaFile(element, (JavaEditor) targetEditor, expr);
        }
    }
    
    private void gotoDeclarationInJavaFile(PsiElement element, JavaEditor editor, JetReferenceExpression expr) throws PartInitException, JavaModelException {
        ICompilationUnit compilationUnit = (ICompilationUnit) editor.getViewPartInput();
        
        if (element instanceof PsiClass) {
            IJavaElement targetJavaElement = getJavaClass(((PsiClass) element).getQualifiedName(), compilationUnit);
            JavaUI.revealInEditor(editor, targetJavaElement);
        } else if (element instanceof PsiMethod) {
            List<IMethod> methods = getJavaMethods((PsiMethod) element, compilationUnit);
            
            if (methods.size() == 1) {
                JavaUI.revealInEditor(editor, (IJavaElement) methods.get(0));
            } else {
                IJavaElement targetJavaElement = selectMethod(methods.toArray(new IMethod[methods.size()]));
                JavaUI.revealInEditor(editor, targetJavaElement);
            }
        }
    }
    
    @NotNull
    private List<IMethod> getJavaMethods(@NotNull PsiMethod psiMethod, @NotNull ICompilationUnit compilationUnit) throws JavaModelException {
        PsiClass psiClass = psiMethod.getContainingClass();
        assert psiClass != null;
        
        IType javaClass = getJavaClass(psiClass.getQualifiedName(), compilationUnit);
        if (javaClass == null) {
            return Collections.emptyList();
        }
        
        List<IMethod> methods = new ArrayList<IMethod>();
        for (IMethod javaMethod : javaClass.getMethods()) {
            if (javaMethod.getElementName().equals(psiMethod.getName())) {
                methods.add(javaMethod);
            }
        }
        
        return methods;
    }
    
    @Nullable
    private IType getJavaClass(@Nullable String qualifiedName, @NotNull ICompilationUnit compilationUnit) throws JavaModelException {
        IType[] types = compilationUnit.getAllTypes();
        for (IType type : types) {
            String javaQualifiedName = type.getFullyQualifiedName();
            if (javaQualifiedName.equals(qualifiedName)) {
                return type;
            }
        }
        
        return null;
    }
    
    private IJavaElement selectMethod(IMethod[] methods) {
        int flags= JavaElementLabelProvider.SHOW_DEFAULT | JavaElementLabelProvider.SHOW_QUALIFIED | JavaElementLabelProvider.SHOW_ROOT;

        ElementListSelectionDialog dialog= new ElementListSelectionDialog(getShell(), new JavaElementLabelProvider(flags));
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
    
    public static IEditorPart openInEditor(IFile file) throws PartInitException {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        IWorkbenchPage page = window.getActivePage();
        
        FileEditorInput fileEditorInput = new FileEditorInput(file);
        IEditorDescriptor defaultEditor = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(file.getName());
        
        return page.openEditor(fileEditorInput, defaultEditor.getId());
    }
    
    @Override
    public void run(IStructuredSelection selection) {
    }
}