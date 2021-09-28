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

import com.intellij.util.SmartList
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*

inline private fun <reified T> ArrayList<KotlinReference>.register(e: KtElement, action: (T) -> KotlinReference) {
    if (e is T) this.add(action(e))
}

inline private fun <reified T> ArrayList<KotlinReference>.registerMulti(
    e: KtElement,
    action: (T) -> List<KotlinReference>
) {
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
                    KotlinSyntheticPropertyAccessorReference.Setter(it)
                )
            }
        }
        register<KtArrayAccessExpression>(element, ::KotlinReferenceExpressionReference)
    }
}

public interface KotlinReference {
    val expression: KtReferenceExpression

    fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor>

    val resolvesByNames: Collection<Name>
}

open class KotlinSimpleNameReference(override val expression: KtSimpleNameExpression) : KotlinReference {
    override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor> {
        return expression.getReferenceTargets(context)
    }

    override val resolvesByNames: Collection<Name>
        get() {
            val element = expression

            if (element is KtOperationReferenceExpression) {
                val tokenType = element.operationSignTokenType
                if (tokenType != null) {
                    val name = OperatorConventions.getNameForOperationSymbol(
                        tokenType, element.parent is KtUnaryExpression, element.parent is KtBinaryExpression
                    ) ?: return emptyList()
                    val counterpart = OperatorConventions.ASSIGNMENT_OPERATION_COUNTERPARTS[tokenType]
                    return if (counterpart != null) {
                        val counterpartName = OperatorConventions.getNameForOperationSymbol(counterpart, false, true)!!
                        listOf(name, counterpartName)
                    } else {
                        listOf(name)
                    }
                }
            }

            return listOf(element.getReferencedNameAsName())
        }
}

public class KotlinInvokeFunctionReference(override val expression: KtCallExpression) : KotlinReference {
    override val resolvesByNames: Collection<Name>
        get() = listOf(OperatorNameConventions.INVOKE)

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

sealed class KotlinSyntheticPropertyAccessorReference(
    override val expression: KtNameReferenceExpression,
    private val getter: Boolean
) : KotlinSimpleNameReference(expression) {
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

    override val resolvesByNames: Collection<Name>
        get() = listOf(expression.getReferencedNameAsName())

    class Getter(expression: KtNameReferenceExpression) : KotlinSyntheticPropertyAccessorReference(expression, true)
    class Setter(expression: KtNameReferenceExpression) : KotlinSyntheticPropertyAccessorReference(expression, false)
}

public class KotlinConstructorDelegationReference(override val expression: KtConstructorDelegationReferenceExpression) :
    KotlinReference {
    override fun getTargetDescriptors(context: BindingContext) = expression.getReferenceTargets(context)

    override val resolvesByNames: Collection<Name>
        get() = emptyList()
}

class KotlinReferenceExpressionReference(override val expression: KtReferenceExpression) : KotlinReference {
    override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor> {
        return expression.getReferenceTargets(context)
    }

    override val resolvesByNames: Collection<Name>
        get() = emptyList()

}

fun KtReferenceExpression.getReferenceTargets(context: BindingContext): Collection<DeclarationDescriptor> {
    val targetDescriptor = context[BindingContext.REFERENCE_TARGET, this]
    return if (targetDescriptor != null) {
        listOf(targetDescriptor)
    } else {
        context[BindingContext.AMBIGUOUS_REFERENCE_TARGET, this].orEmpty()
    }
}