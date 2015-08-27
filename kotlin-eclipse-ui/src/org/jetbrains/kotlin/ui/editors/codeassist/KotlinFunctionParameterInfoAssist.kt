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
package org.jetbrains.kotlin.ui.editors.codeassist

import org.jetbrains.kotlin.ui.editors.KotlinEditor
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.JetValueArgumentList
import org.eclipse.jface.text.contentassist.IContextInformation
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.psi.JetCallElement
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression
import org.jetbrains.kotlin.ui.editors.completion.KotlinCompletionUtils
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.eclipse.swt.graphics.Image
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.types.JetType

public object KotlinFunctionParameterInfoAssist {
    public fun computeContextInformation(editor: KotlinFileEditor, offset: Int): Array<IContextInformation> {
        val psiElement = EditorUtil.getPsiElement(editor, offset)
        val argumentList = PsiTreeUtil.getParentOfType(psiElement, javaClass<JetValueArgumentList>())
        if (argumentList == null) return emptyArray()
        
        val expression = getCallSimpleNameExpression(argumentList)
        if (expression == null) return emptyArray()
        
        val name = expression.getReferencedName()
        
        val variants = KotlinCompletionUtils.getReferenceVariants(expression, name, EditorUtil.getFile(editor)!!)
        
        return variants
                .flatMap { 
                    when (it) {
                        is FunctionDescriptor -> listOf(it)
                        is ClassDescriptor -> it.getConstructors()
                        else -> emptyList<FunctionDescriptor>()
                    }
                }
                .filter { it.getValueParameters().isNotEmpty() }
                .map { KotlinFunctionParameterContextInformation(it) }
                .toTypedArray()
    }
}

fun getCallSimpleNameExpression(argumentList: JetValueArgumentList): JetSimpleNameExpression? {
    val argumentListParent = argumentList.getParent()
    return if (argumentListParent is JetCallElement) argumentListParent.getCallNameExpression() else null
}

public class KotlinFunctionParameterContextInformation(descriptor: FunctionDescriptor) : IContextInformation {
    val displayString = DescriptorRenderer.ONLY_NAMES_WITH_SHORT_TYPES.render(descriptor)
    val informationString = descriptor.getValueParameters().joinToString(", ") { renderParameter(it) }
    
    override fun getContextDisplayString(): String = displayString
    
    override fun getImage(): Image? = null
    
    override fun getInformationDisplayString(): String = informationString
    
    private fun renderParameter(parameter: ValueParameterDescriptor): String {
        val result = StringBuilder()
        
        if (parameter.getVarargElementType() != null) result.append("vararg ")
        result.append(parameter.getName())
                .append(": ")
                .append(DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(getActualParameterType(parameter)));
        
        return result.toString()
    }
    
    private fun getActualParameterType(descriptor: ValueParameterDescriptor): JetType {
        return descriptor.getVarargElementType() ?: descriptor.getType()
    }
}