package org.jetbrains.kotlin.ui.editors.quickassist

import com.intellij.psi.PsiElement
import org.eclipse.jface.text.IDocument
import org.jetbrains.kotlin.psi.JetClassOrObject
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer
import org.eclipse.jdt.core.JavaCore
import org.jetbrains.kotlin.resolve.BindingContextUtils
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.resolve.OverrideResolver
import com.intellij.psi.util.PsiTreeUtil
import org.eclipse.jface.dialogs.MessageDialog
import org.jetbrains.kotlin.psi.JetFile
import java.util.ArrayList
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.psi.JetClassBody
import com.intellij.psi.PsiWhiteSpace
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.psi.JetNamedFunction
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.renderer.DescriptorRendererBuilder
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.NameShortness
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.eclipse.jface.text.TextUtilities
import org.jetbrains.kotlin.ui.formatter.AlignmentStrategy
import org.jetbrains.kotlin.ui.editors.KotlinEditor
import org.jetbrains.kotlin.eclipse.ui.utils.IndenterUtil
import org.jetbrains.kotlin.psi.JetNamedDeclaration
import org.jetbrains.kotlin.psi.JetDeclaration

public class KotlinImplementMethodsProposal : KotlinOverrideImplementMethodsProposal() {
    override public fun collectMethodsToGenerate(classOrObject: JetClassOrObject): Set<CallableMemberDescriptor> {
        val descriptor = classOrObject.resolveToDescriptor()
        if (descriptor is ClassDescriptor) {
            return OverrideResolver.getMissingImplementations(descriptor)
        }
        return emptySet()
    }
    
    override fun getDisplayString(): String = "Implement methods"
}