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
package org.jetbrains.kotlin.core.references

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.core.resolve.EclipseDescriptorUtils
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.psi.KtConstructorDelegationReferenceExpression
import org.eclipse.jdt.core.IJavaProject
import org.jetbrains.kotlin.psi.KtElement
import java.util.ArrayList
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor
import com.intellij.util.SmartList
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.lexer.KtTokens

inline private fun <reified T> ArrayList<KotlinReference>.register(e: KtElement, action: (T) -> KotlinReference) {
    if (e is T) this.add(action(e))
}

inline private fun <reified T> ArrayList<KotlinReference>.registerMulti(e: KtElement, action: (T) -> List<KotlinReference>) {
    if (e is T) this.addAll(action(e))
}

public fun createReferences(element: KtReferenceExpression): List<KotlinReference> {
    return arrayListOf<KotlinReference>().apply {
        register<KtSimpleNameExpression>(element, ::KotlinSimpleNameReference)
        
        register<KtCallExpression>(element, ::KotlinInvokeFunctionReference)
        
        register<KtConstructorDelegationReferenceExpression>(element, ::KotlinConstructorDelegationReference)
        
        registerMulti<KtNameReferenceExpression>(element) {
            if (it.getReferencedNameElementType() != KtTokens.IDENTIFIER) return@registerMulti emptyList()
            
            when (it.readWriteAccess()) {
                ReferenceAccess.READ -> listOf(KotlinSyntheticPropertyAccessorReference.Getter(it))
                ReferenceAccess.WRITE -> listOf(KotlinSyntheticPropertyAccessorReference.Setter(it))
                ReferenceAccess.READ_WRITE -> listOf(
                            KotlinSyntheticPropertyAccessorReference.Getter(it), 
                            KotlinSyntheticPropertyAccessorReference.Setter(it))
            }
        }
    }
}

public interface KotlinReference {
    val expression: KtReferenceExpression
    
    fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor>
}

open class KotlinSimpleNameReference(override val expression: KtSimpleNameExpression) : KotlinReference {
    override fun getTargetDescriptors(context: BindingContext) = expression.getReferenceTargets(context)
}

public class KotlinInvokeFunctionReference(override val expression: KtCallExpression) : KotlinReference {
    override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor> {
        val call = expression.getCall(context)
        val resolvedCall = call.getResolvedCall(context)
        return when {
            resolvedCall is VariableAsFunctionResolvedCall -> listOf(resolvedCall.functionCall.getCandidateDescriptor())
            call != null && resolvedCall != null && call.getCallType() == Call.CallType.INVOKE -> listOf(resolvedCall.getCandidateDescriptor())
            else -> emptyList()
        }
    }
}

sealed class KotlinSyntheticPropertyAccessorReference(override val expression: KtNameReferenceExpression, private val getter: Boolean) 
        : KotlinSimpleNameReference(expression) {
    override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor> {
        val descriptors = super.getTargetDescriptors(context)
        if (descriptors.none { it is SyntheticJavaPropertyDescriptor }) return emptyList()
        
        val result = SmartList<FunctionDescriptor>()
        for (descriptor in descriptors) {
            if (descriptor is SyntheticJavaPropertyDescriptor) {
                if (getter) {
                    result.add(descriptor.getMethod)
                } else {
                    result.addIfNotNull(descriptor.setMethod)
                }
            }
        }
        return result
    }
    
    class Getter(expression: KtNameReferenceExpression) : KotlinSyntheticPropertyAccessorReference(expression, true)
    class Setter(expression: KtNameReferenceExpression) : KotlinSyntheticPropertyAccessorReference(expression, false)
}

public class KotlinConstructorDelegationReference(override val expression: KtConstructorDelegationReferenceExpression) : KotlinReference {
    override fun getTargetDescriptors(context: BindingContext) = expression.getReferenceTargets(context)
}

fun KtReferenceExpression.getReferenceTargets(context: BindingContext): Collection<DeclarationDescriptor> {
    val targetDescriptor = context[BindingContext.REFERENCE_TARGET, this]
    return if (targetDescriptor != null) {
            listOf(targetDescriptor) 
        } else {
            context[BindingContext.AMBIGUOUS_REFERENCE_TARGET, this].orEmpty()
        }
}