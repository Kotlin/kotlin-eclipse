package org.jetbrains.kotlin.ui.editors.quickassist;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.kotlin.descriptors.VariableDescriptor;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers;
import org.jetbrains.kotlin.psi.JetFunction;
import org.jetbrains.kotlin.psi.JetNamedDeclaration;
import org.jetbrains.kotlin.psi.JetNamedFunction;
import org.jetbrains.kotlin.psi.JetParameter;
import org.jetbrains.kotlin.psi.JetParameterList;
import org.jetbrains.kotlin.psi.JetProperty;
import org.jetbrains.kotlin.psi.JetTypeReference;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.types.ErrorUtils;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.ui.editors.KotlinEditor;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;

public class KotlinSpecifyTypeAssistProposal extends KotlinQuickAssistProposal {
    
    private static final String SPECIFY_TYPE_EXPLICITLY_MESSAGE = "Specify type explicitly";
    private static final String REMOVE_TYPE_MESSAGE = "Remove explicitly specified type";
    
    private String displayString = SPECIFY_TYPE_EXPLICITLY_MESSAGE;

    @Override
    public void apply(@NotNull IDocument document, @NotNull PsiElement element) {
        JetTypeReference typeRefParent = PsiTreeUtil.getTopmostParentOfType(element, JetTypeReference.class);
        if (typeRefParent != null) {
            element = typeRefParent;
        }
        
        PsiElement parent = element.getParent();
        JetType type = getTypeForDeclaration((JetNamedDeclaration) parent);
        if (parent instanceof JetProperty) {
            JetProperty property = (JetProperty) parent;
            if (property.getTypeReference() == null) {
                addTypeAnnotation(document, property, type);
            } else {
                removeTypeAnnotation(document, property);
            }
        } 
        else if (parent instanceof JetParameter) {
            JetParameter parameter = (JetParameter) parent;
            if (parameter.getTypeReference() == null) {
                addTypeAnnotation(document, parameter, type);
            } else {
                removeTypeAnnotation(document, parameter);
            }
        }
        else if (parent instanceof JetNamedFunction) {
            JetNamedFunction function = (JetNamedFunction) parent;
            assert function.getTypeReference() == null;
            addTypeAnnotation(document, function, type);
        } else {
            throw new IllegalStateException("Unexpected parent " + parent);
        }
    }
    
    private void addTypeAnnotationToElement(@NotNull IDocument document, @NotNull PsiElement anchor, @NotNull JetType exprType) {
        KotlinEditor editor = getActiveEditor();
        if (editor == null) return;
        
        try {
            document.replace(getEndOffset(anchor, editor), 0, ": " + IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(exprType)); 
        } catch (BadLocationException e) {
            KotlinLogger.logAndThrow(e);
        }
    }
    
    private void removeTypeAnnotationFromElement(@NotNull IDocument document, @NotNull PsiElement removeAfter, @NotNull JetTypeReference typeReference) {
        KotlinEditor editor = getActiveEditor();
        if (editor == null) return;
        
        try {
            int endOffset = getEndOffset(removeAfter, editor);
            int endOfType = getEndOffset(typeReference, editor);
            document.replace(endOffset, endOfType - endOffset, ""); 
        } catch (BadLocationException e) {
            KotlinLogger.logAndThrow(e);
        }
    }
    
    private void removeTypeAnnotation(@NotNull IDocument document, @NotNull JetProperty property) {
        removeTypeAnnotation(document, property.getNameIdentifier(), property.getTypeReference());
    }
    
    private void removeTypeAnnotation(@NotNull IDocument document, @NotNull JetParameter parameter) {
        removeTypeAnnotation(document, parameter.getNameIdentifier(), parameter.getTypeReference());
    }
    
    private void removeTypeAnnotation(@NotNull IDocument document, @Nullable PsiElement removeAfter, @Nullable JetTypeReference typeReference) {
        if (removeAfter == null || typeReference == null) {
            return;
        }
        
        PsiElement sibling = removeAfter.getNextSibling();
        if (sibling == null) {
            return;
        }
        
        removeTypeAnnotationFromElement(document, removeAfter, typeReference);
    }
    
    @Override
    public boolean isApplicable(@NotNull PsiElement element) {
        JetTypeReference typeRefParent = PsiTreeUtil.getTopmostParentOfType(element, JetTypeReference.class);
        if (typeRefParent != null) {
            element = typeRefParent;
        }
        PsiElement parent = element.getParent();
        if (!(parent instanceof JetNamedDeclaration)) {
            return false;
        }
        JetNamedDeclaration declaration = (JetNamedDeclaration) parent;

        if (declaration instanceof JetProperty && !PsiTreeUtil.isAncestor(((JetProperty) declaration).getInitializer(), element, false)) {
            if (((JetProperty) declaration).getTypeReference() != null) {
                setDisplayString(REMOVE_TYPE_MESSAGE);
                return true;
            } else {
                setDisplayString(SPECIFY_TYPE_EXPLICITLY_MESSAGE);
            }
        }
        else if (declaration instanceof JetNamedFunction && ((JetNamedFunction) declaration).getTypeReference() == null
                && !((JetNamedFunction) declaration).hasBlockBody()) {
            setDisplayString(SPECIFY_TYPE_EXPLICITLY_MESSAGE);
        }
        else if (declaration instanceof JetParameter && ((JetParameter) declaration).isLoopParameter()) {
            if (((JetParameter) declaration).getTypeReference() != null) {
                setDisplayString(REMOVE_TYPE_MESSAGE);
                return true;
            } else {
                setDisplayString(SPECIFY_TYPE_EXPLICITLY_MESSAGE);
            }
        }
        else {
            return false;
        }

        if (getTypeForDeclaration(declaration).isError()) {
            return false;
        }
        
        return !hasPublicMemberDiagnostic(declaration);
    }
    
    private boolean hasPublicMemberDiagnostic(@NotNull JetNamedDeclaration declaration) {
        IFile file = getActiveFile();
        assert file != null;
        
        IJavaProject javaProject = JavaCore.create(file.getProject());
        
        BindingContext bindingContext = KotlinAnalyzer
                .analyzeOneFileCompletely(javaProject, declaration.getContainingJetFile())
                .getBindingContext();
        for (Diagnostic diagnostic : bindingContext.getDiagnostics()) {
            if (Errors.PUBLIC_MEMBER_SHOULD_SPECIFY_TYPE == diagnostic.getFactory() && declaration == diagnostic.getPsiElement()) {
                return true;
            }
        }
        
        return false;
    }
    
    private void addTypeAnnotation(@NotNull IDocument document, @NotNull JetFunction function, @NotNull JetType exprType) {
        JetParameterList valueParameterList = function.getValueParameterList();
        assert valueParameterList != null;
        addTypeAnnotation(document, function, valueParameterList, exprType);
    }
    
    private void addTypeAnnotation(@NotNull IDocument document, @NotNull JetParameter parameter, @NotNull JetType exprType) {
        addTypeAnnotation(document, parameter, parameter.getNameIdentifier(), exprType);
    }
    
    private void addTypeAnnotation(@NotNull IDocument document, @NotNull JetProperty property, @NotNull JetType exprType) {
        if (property.getTypeReference() != null) {
            return;
        }
        
        PsiElement anchor = property.getNameIdentifier();
        if (anchor == null) {
            return;
        }
        
        addTypeAnnotation(document, property, anchor, exprType);
    }
    
    private void addTypeAnnotation(@NotNull IDocument document, @NotNull JetNamedDeclaration namedDeclaration, @NotNull PsiElement anchor, @NotNull JetType exprType) {
        assert !exprType.isError() : "Unexpected error type: " + namedDeclaration.getText();
        addTypeAnnotationToElement(document, anchor, exprType);
    }
    
    @NotNull
    private JetType getTypeForDeclaration(@NotNull JetNamedDeclaration declaration) {
        IFile file = getActiveFile();
        assert (file != null);
        
        IJavaProject javaProject = JavaCore.create(file.getProject());
        
        BindingContext bindingContext = KotlinAnalyzer
                .analyzeOneFileCompletely(javaProject, declaration.getContainingJetFile())
                .getBindingContext();
        DeclarationDescriptor descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, declaration);

        JetType type;
        if (descriptor instanceof VariableDescriptor) {
            type = ((VariableDescriptor) descriptor).getType();
        }
        else if (descriptor instanceof SimpleFunctionDescriptor) {
            type = ((SimpleFunctionDescriptor) descriptor).getReturnType();
        }
        else {
            return ErrorUtils.createErrorType("unknown declaration type");
        }

        return type == null ? ErrorUtils.createErrorType("null type") : type;
    }

    @Override
    public String getDisplayString() {
        return displayString;
    }
    
    private void setDisplayString(@NotNull String displayString) {
        this.displayString = displayString;
    }
}