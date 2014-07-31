package org.jetbrains.kotlin.ui.editors.quickassist;

import java.util.List;
import java.util.ListIterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetCallExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetQualifiedExpression;
import org.jetbrains.jet.lang.psi.PsiPackage;
import org.jetbrains.jet.lang.psi.ValueArgument;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.calls.callUtil.CallUtilPackage;
import org.jetbrains.jet.lang.resolve.calls.model.DefaultValueArgument;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedValueArgument;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer;
import org.jetbrains.kotlin.ui.editors.KotlinEditor;
import org.jetbrains.kotlin.utils.EditorUtil;
import org.jetbrains.kotlin.utils.IndenterUtil;

import com.google.common.collect.Lists;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;

public class KotlinReplaceGetAssistProposal extends KotlinQuickAssistProposal {
    
    @Override
    public void apply(@NotNull IDocument document, @NotNull PsiElement psiElement) {
        JetCallExpression callElement = PsiTreeUtil.getParentOfType(psiElement, JetCallExpression.class);
        if (callElement == null) {
            return;
        }
        
        JetQualifiedExpression qualifiedExpression = PsiTreeUtil.getParentOfType(psiElement, JetQualifiedExpression.class);
        if (qualifiedExpression == null) {
            return;
        }
        
        IFile file = EditorUtil.getFile(getActiveEditor());
        String arguments = getArguments(qualifiedExpression, file);
        if (arguments == null) {
            return;
        }
        
        replaceGetForElement(qualifiedExpression, arguments);
    }
    
    private void replaceGetForElement(@NotNull JetQualifiedExpression element, @NotNull String arguments) {
        KotlinEditor editor = getActiveEditor();
        if (editor == null) {
            return;
        }
        
        IDocument document = editor.getViewer().getDocument();
        
        try {
            JetExpression indexExpression = PsiPackage.JetPsiFactory(element).createExpression(
                    element.getReceiverExpression().getText() + "[" + arguments + "]");
            
            document.replace(
                    getStartOffset(element, editor), 
                    element.getTextLength() + IndenterUtil.getLineSeparatorsOccurences(element.getText()), 
                    indexExpression.getText());
        } catch (BadLocationException e) {
            KotlinLogger.logAndThrow(e);
        }
    }
    
    @Nullable
    private String getArguments(@NotNull JetQualifiedExpression element, @NotNull IFile file) {
        StringBuilder buffer = new StringBuilder();
        List<ValueArgument> valueArguments = getPositionalArguments(element, file);
        
        if (valueArguments == null) return null;
        
        boolean firstArgument = true;
        for (ValueArgument valueArgument : valueArguments) {
            if (!firstArgument) buffer.append(", ");
            firstArgument = false;
            
            JetExpression argumentExpression = valueArgument.getArgumentExpression();
            if (argumentExpression == null) continue;
            buffer.append(argumentExpression.getText());
        }
        
        return buffer.toString();
    }
    
    @Nullable
    public static List<ValueArgument> getPositionalArguments(@NotNull JetQualifiedExpression element, @NotNull IFile file) {
        ResolvedCall<?> resolvedCall = getResolvedCall(element, file);
        if (resolvedCall == null) return null;
        
        List<ResolvedValueArgument> resolvedValueArguments = resolvedCall.getValueArgumentsByIndex();
        if (resolvedValueArguments == null) return null;
        
        int indexOfFirstDefaultArgument = resolvedValueArguments.indexOf(DefaultValueArgument.DEFAULT);
        
        List<ResolvedValueArgument> valueArgumentGroups = Lists.newArrayList();
        if (indexOfFirstDefaultArgument >= 0) {
            ListIterator<ResolvedValueArgument> iter = resolvedValueArguments.listIterator(indexOfFirstDefaultArgument);
            while (iter.hasNext()) {
                ResolvedValueArgument valueArgument = iter.next();
                if (valueArgument != DefaultValueArgument.DEFAULT) {
                    return null; 
                }
            }
            
            valueArgumentGroups = resolvedValueArguments.subList(0, indexOfFirstDefaultArgument);
        } else {
            valueArgumentGroups = resolvedValueArguments;
        }
        
        List<ValueArgument> valueArguments = Lists.newArrayList();
        for (ResolvedValueArgument valueArgumentGroup : valueArgumentGroups) {
            for (ValueArgument va : valueArgumentGroup.getArguments()) {
                valueArguments.add(va);
            }
        }
        
        return valueArguments;
    }
    
    @Nullable
    private static ResolvedCall<?> getResolvedCall(@NotNull JetQualifiedExpression element, @NotNull IFile file) {
        JetExpression call = element.getSelectorExpression();
        if (!(call instanceof JetCallExpression)) return null;
        
        JetCallExpression jetCallExpression = (JetCallExpression) call; 
        
        IJavaProject javaProject = JavaCore.create(file.getProject());
        BindingContext bindingContext = KotlinAnalyzer.analyzeOnlyOneFileCompletely(javaProject, KotlinPsiManager.INSTANCE.getParsedFile(file));
        
        return CallUtilPackage.getResolvedCall(jetCallExpression.getCalleeExpression(), bindingContext);
    }
    
    @Override
    public boolean isApplicable(@NotNull PsiElement psiElement) {
        JetCallExpression callExpression = PsiTreeUtil.getParentOfType(psiElement, JetCallExpression.class);
        if (callExpression == null) {
            return false;
        }
        
        JetExpression calleeExpression = callExpression.getCalleeExpression();

        if (calleeExpression == null) {
            return false;
        }

        return "get".equals(calleeExpression.getText()) && callExpression.getTypeArgumentList() == null &&
                callExpression.getValueArguments().size() + callExpression.getFunctionLiteralArguments().size() > 0;
    }

    @Override
    public String getDisplayString() {
        return "Replace 'get' with index operator";
    }
}