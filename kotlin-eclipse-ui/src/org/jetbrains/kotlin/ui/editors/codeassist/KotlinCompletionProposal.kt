/*******************************************************************************
 * Copyright 2000-2016 JetBrains s.r.o.
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

import org.eclipse.core.resources.IFile
import org.eclipse.jdt.core.search.TypeNameMatch
import org.eclipse.jdt.ui.JavaElementLabels
import org.eclipse.jface.text.DocumentEvent
import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.ITextViewer
import org.eclipse.jface.text.contentassist.ICompletionProposal
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension6
import org.eclipse.jface.text.contentassist.IContextInformation
import org.eclipse.jface.viewers.StyledString
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.graphics.Point
import org.jetbrains.kotlin.builtins.isExtensionFunctionType
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.core.imports.FunctionCandidate
import org.jetbrains.kotlin.core.imports.TypeCandidate
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.calls.util.getValueParametersCountFromFunctionType
import org.jetbrains.kotlin.ui.editors.codeassist.CaretPosition.AFTER_BRACKETS
import org.jetbrains.kotlin.ui.editors.codeassist.CaretPosition.IN_BRACKETS
import org.jetbrains.kotlin.ui.editors.completion.KotlinCompletionUtils
import org.jetbrains.kotlin.ui.editors.quickfix.placeImports

fun withKotlinInsertHandler(
    descriptor: DeclarationDescriptor,
    proposal: KotlinCompletionProposal
): ICompletionProposal {
    return when (descriptor) {
        is FunctionDescriptor -> {
            val parameters = descriptor.valueParameters
            when (parameters.size) {
                0 -> KotlinFunctionCompletionProposal(proposal, AFTER_BRACKETS, false)

                1 -> {
                    val parameter = parameters.single()
                    val parameterType = parameter.type
                    if (parameterType.isFunctionType || parameterType.isExtensionFunctionType) {
                        val parameterCount = getValueParametersCountFromFunctionType(parameterType)
                        return if (parameterCount <= 1) {
                            KotlinFunctionCompletionProposal(proposal, IN_BRACKETS, true)
                        } else {
                            val tempParamNames = getLambdaParamNames(parameter)
                            KotlinFunctionCompletionProposal(proposal, IN_BRACKETS, true, tempParamNames)
                        }
                    }
                    KotlinFunctionCompletionProposal(proposal, IN_BRACKETS, false)
                }

                else -> KotlinFunctionCompletionProposal(proposal, IN_BRACKETS, false)
            }
        }

        else -> proposal
    }
}

private fun getLambdaParamNames(parameter: ValueParameterDescriptor): String {
    val typeCharMap = mutableMapOf<Char, Int>()
    fun Char.nextParamName(): String {
        val tempChar = lowercaseChar()
        val tempCurrentNum = typeCharMap[tempChar]
        return if (tempCurrentNum == null) {
            typeCharMap[tempChar] = 2
            "$tempChar"
        } else {
            typeCharMap[tempChar] = tempCurrentNum + 1
            "$tempChar$tempCurrentNum"
        }
    }

    val tempParamNames =
        parameter.type.arguments.dropLast(1).joinToString(", ", postfix = " -> ") {
            val tempAnnotation =
                it.type.annotations.findAnnotation(FqName(ParameterName::class.qualifiedName!!))
            tempAnnotation?.allValueArguments?.get(Name.identifier(ParameterName::name.name))
                ?.value?.toString() ?: it.type.toString().first().nextParamName()
        }
    return tempParamNames
}

fun getIdentifierInfo(document: IDocument, offset: Int): IdentifierInfo {
    val text = document.get()
    var identStartOffset = offset
    while ((identStartOffset != 0) && Character.isUnicodeIdentifierPart(text[identStartOffset - 1])) {
        identStartOffset--
    }
    return IdentifierInfo(text!!.substring(identStartOffset, offset), identStartOffset)
}

data class IdentifierInfo(val identifierPart: String, val identifierStart: Int)

open class KotlinCompletionProposal constructor(
    val replacementString: String,
    private val img: Image?,
    private val presentableString: String,
    private val containmentPresentableString: String? = null,
    private val information: IContextInformation? = null,
    private val additionalInfo: String? = null,
    @Volatile private var identifierPart: String
) : ICompletionProposal, ICompletionProposalExtension2, ICompletionProposalExtension6 {

    private var selectedOffset = -1

    open fun getRelevance(): Int {
        return computeCaseMatchingRelevance(identifierPart.toCharArray(), replacementString.toCharArray())
    }

    override fun apply(viewer: ITextViewer, trigger: Char, stateMask: Int, offset: Int) {
        val document = viewer.document
        val (identifierPart, identifierStart) = getIdentifierInfo(document, offset)
        document.replace(identifierStart, offset - identifierStart, replacementString)

        selectedOffset = offset - identifierPart.length + replacementString.length
    }

    override fun validate(document: IDocument, offset: Int, event: DocumentEvent): Boolean {
        val identiferInfo = getIdentifierInfo(document, offset)
        identifierPart = identiferInfo.identifierPart
        return KotlinCompletionUtils.applicableNameFor(identiferInfo.identifierPart, replacementString)
    }

    override fun getSelection(document: IDocument): Point? = Point(selectedOffset, 0)

    override fun getAdditionalProposalInfo(): String? = additionalInfo

    override fun getDisplayString(): String = presentableString

    override fun getImage(): Image? = img

    override fun getContextInformation(): IContextInformation? = information

    override fun getStyledDisplayString(): StyledString {
        return if (containmentPresentableString != null) {
            createStyledString(displayString, containmentPresentableString)
        } else {
            StyledString(displayString)
        }
    }

    override fun selected(viewer: ITextViewer?, smartToggle: Boolean) {
    }

    override fun unselected(viewer: ITextViewer?) {
    }

    final override fun apply(document: IDocument) {
        // should not be called
    }
}

class KotlinImportTypeCompletionProposal(
    private val typeName: TypeNameMatch,
    image: Image?,
    val file: IFile,
    identifierPart: String
) :
    KotlinCompletionProposal(
        typeName.simpleTypeName,
        image,
        typeName.simpleTypeName,
        typeName.fullyQualifiedName.removeSuffix(".${typeName.simpleTypeName}"),
        identifierPart = identifierPart
    ) {

    private var importShift = -1

    override fun apply(viewer: ITextViewer, trigger: Char, stateMask: Int, offset: Int) {
        super.apply(viewer, trigger, stateMask, offset)
        importShift = placeImports(listOf(TypeCandidate(typeName)), file, viewer.document)
    }

    override fun getSelection(document: IDocument): Point? {
        val selection = super.getSelection(document)
        return if (importShift > 0 && selection != null) Point(selection.x + importShift, 0) else selection
    }

    override fun getRelevance(): Int {
        return -1
    }
}

class KotlinImportCallableCompletionProposal(
    val descriptor: CallableDescriptor,
    image: Image?,
    val file: IFile,
    identifierPart: String
) :
    KotlinCompletionProposal(
        "${descriptor.name.identifier}${if (descriptor is PropertyDescriptor) "" else "()"}",
        image,
        DescriptorRenderer.SHORT_NAMES_IN_TYPES.render(descriptor),
        null,
        identifierPart = identifierPart
    ) {

    private var importShift = -1

    override fun apply(viewer: ITextViewer, trigger: Char, stateMask: Int, offset: Int) {
        super.apply(viewer, trigger, stateMask, offset)
        importShift = placeImports(listOf(FunctionCandidate(descriptor)), file, viewer.document)
    }

    override fun getSelection(document: IDocument): Point? {
        val selection = super.getSelection(document)
        return if (importShift > 0 && selection != null) Point(selection.x + importShift, 0) else selection
    }

    override fun getRelevance(): Int = -1
}

class KotlinKeywordCompletionProposal(keyword: String, identifierPart: String) :
    KotlinCompletionProposal(keyword, null, keyword, identifierPart = identifierPart)

private fun createStyledString(simpleName: String, containingDeclaration: String): StyledString {
    return StyledString().apply {
        append(simpleName)
        if (containingDeclaration.isNotBlank()) {
            append(JavaElementLabels.CONCAT_STRING, StyledString.QUALIFIER_STYLER)
            append(containingDeclaration, StyledString.QUALIFIER_STYLER)
        }
    }
}
