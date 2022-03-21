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

import com.intellij.psi.util.PsiTreeUtil
import org.eclipse.jface.text.contentassist.IContextInformation
import org.eclipse.swt.graphics.Image
import org.jetbrains.kotlin.core.resolve.EclipseDescriptorUtils
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import org.jetbrains.kotlin.eclipse.ui.utils.KotlinImageProvider
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.descriptorUtil.declaresOrInheritsDefaultValue
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.ui.editors.KotlinEditor
import org.jetbrains.kotlin.ui.editors.completion.KotlinCompletionUtils

object KotlinFunctionParameterInfoAssist {
    fun computeContextInformation(editor: KotlinEditor, offset: Int): Array<IContextInformation> {
        val file = editor.eclipseFile ?: throw IllegalStateException("Failed to retrieve IFile from editor $editor")
        val ktFile = editor.parsedFile ?: throw IllegalStateException("Failed to retrieve KTFile from editor $editor")
        val javaProject = editor.javaProject ?: throw IllegalStateException("Failed to retrieve JavaProject from editor $editor")

        val expression = getCallSimpleNameExpression(editor, offset) ?: return emptyArray()

        val referencedName = expression.getReferencedName()

        val nameFilter: (Name) -> Boolean = { name -> name.asString() == referencedName }
        val variants = KotlinCompletionUtils.getReferenceVariants(
            expression,
            nameFilter,
            ktFile,
            file,
            referencedName,
            javaProject
        )
        
        return variants.map { it.descriptor }
                .flatMap { 
                    when (it) {
                        is FunctionDescriptor -> listOf(it)
                        is ClassDescriptor -> it.constructors
                        else -> emptyList<FunctionDescriptor>()
                    }
                }
                .filter { it.valueParameters.isNotEmpty() }
                .map { KotlinFunctionParameterContextInformation(it) }
                .toTypedArray()
    }
}

fun getCallSimpleNameExpression(editor: KotlinEditor, offset: Int): KtSimpleNameExpression? {
    val psiElement = EditorUtil.getPsiElement(editor, offset)
    val argumentList = PsiTreeUtil.getParentOfType(psiElement, KtValueArgumentList::class.java)
    if (argumentList == null) return null
    
    val argumentListParent = argumentList.parent
    return if (argumentListParent is KtCallElement) argumentListParent.getCallNameExpression() else null
}

class KotlinFunctionParameterContextInformation(descriptor: FunctionDescriptor) : IContextInformation {
    val displayString = DescriptorRenderer.ONLY_NAMES_WITH_SHORT_TYPES.render(descriptor)
    val renderedParameters = descriptor.valueParameters.map { renderParameter(it) }
    val informationString = renderedParameters.joinToString(", ")
    val displayImage = KotlinImageProvider.getImage(descriptor)
    val name = if (descriptor is ConstructorDescriptor) descriptor.containingDeclaration.name else descriptor.name
    
    override fun getContextDisplayString(): String = displayString
    
    override fun getImage(): Image? = displayImage
    
    override fun getInformationDisplayString(): String = informationString
    
    private fun renderParameter(parameter: ValueParameterDescriptor): String {
        val result = StringBuilder()
        
        if (parameter.varargElementType != null) result.append("vararg ")
        result.append(parameter.name)
                .append(": ")
                .append(DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(getActualParameterType(parameter)))
        
        if (parameter.declaresOrInheritsDefaultValue()) {
            val parameterDeclaration = EclipseDescriptorUtils.descriptorToDeclaration(parameter)
            if (parameterDeclaration != null) {
                result.append(" = ${getDefaultExpressionString(parameterDeclaration)}")
            }
        }
        
        return result.toString()
    }
    
    private fun getDefaultExpressionString(parameterDeclaration: SourceElement): String {
        val parameterText: String? = if (parameterDeclaration is KotlinSourceElement) {
                val parameter = parameterDeclaration.psi
                (parameter as? KtParameter)?.defaultValue?.text
            } else {
                null
            }
        
        return parameterText ?: "..."
    }
    
    private fun getActualParameterType(descriptor: ValueParameterDescriptor): KotlinType {
        return descriptor.varargElementType ?: descriptor.type
    }
}
