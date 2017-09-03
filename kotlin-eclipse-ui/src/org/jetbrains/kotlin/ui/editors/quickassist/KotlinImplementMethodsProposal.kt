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
package org.jetbrains.kotlin.ui.editors.quickassist

import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.TextUtilities
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.renderer.ClassifierNamePolicy
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.DescriptorRendererModifier
import org.jetbrains.kotlin.renderer.OverrideRenderingPolicy
import org.jetbrains.kotlin.resolve.OverrideResolver
import org.jetbrains.kotlin.ui.editors.KotlinEditor
import org.jetbrains.kotlin.ui.formatter.EclipseDocumentRange
import org.jetbrains.kotlin.ui.formatter.formatRange
import java.util.ArrayList

private const val DEFAULT_EXCEPTION_CALL = "TODO()"
private const val DEFAULT_PROPERTY_BODY = "TODO()"

public class KotlinImplementMethodsProposal(
        editor: KotlinEditor,
        private val functionBody: String = DEFAULT_EXCEPTION_CALL,
        private val propertyBody: String = DEFAULT_PROPERTY_BODY) : KotlinQuickAssistProposal(editor) {
    private val OVERRIDE_RENDERER = DescriptorRenderer.withOptions {
        defaultParameterValueRenderer = null
        modifiers = setOf(DescriptorRendererModifier.OVERRIDE)
        withDefinedIn = false
        classifierNamePolicy = ClassifierNamePolicy.SHORT
        overrideRenderingPolicy = OverrideRenderingPolicy.RENDER_OVERRIDE
        unitReturnType = false
        typeNormalizer = IdeDescriptorRenderers.APPROXIMATE_FLEXIBLE_TYPES
    }

    override fun apply(document: IDocument, psiElement: PsiElement) {
        val classOrObject = PsiTreeUtil.getParentOfType(psiElement, KtClassOrObject::class.java, false)
        if (classOrObject == null) return

        val missingImplementations = collectMethodsToGenerate(classOrObject)
        if (missingImplementations.isEmpty()) {
            return
        }

        generateMethods(document, classOrObject, missingImplementations)
    }

    override fun getDisplayString(): String = "Implement Members"

    override fun isApplicable(psiElement: PsiElement): Boolean {
        val classOrObject = PsiTreeUtil.getParentOfType(psiElement, KtClassOrObject::class.java, false)
        if (classOrObject != null) {
            return collectMethodsToGenerate(classOrObject).isNotEmpty()
        }

        return false
    }

    public fun generateMethods(document: IDocument, classOrObject: KtClassOrObject, selectedElements: Set<CallableMemberDescriptor>) {
        var body = classOrObject.getBody()
        val psiFactory = KtPsiFactory(classOrObject.getProject())
        if (body == null) {
            val bodyText = "${psiFactory.createWhiteSpace().getText()}${psiFactory.createEmptyClassBody().getText()}"
            insertAfter(classOrObject, bodyText)
        } else {
            removeWhitespaceAfterLBrace(body, editor.document, editor)
        }

        val insertOffset = findLBraceEndOffset(editor.document, getStartOffset(classOrObject, editor))
        if (insertOffset == null) return

        val lineDelimiter = TextUtilities.getDefaultLineDelimiter(editor.document)

        val generatedText = generateOverridingMembers(selectedElements, classOrObject, lineDelimiter)
                .map { it.node.text }
                .joinToString(lineDelimiter, postfix = lineDelimiter)

        document.replace(insertOffset, 0, generatedText)
        
        val file = editor.eclipseFile ?: return
        formatRange(
                document,
                EclipseDocumentRange(insertOffset, insertOffset + generatedText.length),
                psiFactory,
                file.name)
    }

    private fun removeWhitespaceAfterLBrace(body: KtClassBody, document: IDocument, editor: KotlinEditor) {
        val lBrace = body.lBrace
        if (lBrace != null) {
            val sibling = lBrace.getNextSibling()
            val needNewLine = sibling.getNextSibling() is KtDeclaration
            if (sibling is PsiWhiteSpace && !needNewLine) {
                document.replace(getStartOffset(sibling, editor), sibling.getTextLength(), "")
            }
        }
    }

    private fun findLBraceEndOffset(document: IDocument, startIndex: Int): Int? {
        val text = document.get()
        for (i in startIndex..text.lastIndex) {
            if (text[i] == '{') return i + 1
        }

        return null
    }

    private fun generateOverridingMembers(selectedElements: Set<CallableMemberDescriptor>,
                                          classOrObject: KtClassOrObject,
                                          lineDelimiter: String): List<KtElement> {
        val overridingMembers = ArrayList<KtElement>()
        for (selectedElement in selectedElements) {
            if (selectedElement is SimpleFunctionDescriptor) {
                overridingMembers.add(overrideFunction(classOrObject, selectedElement, lineDelimiter))
            } else if (selectedElement is PropertyDescriptor) {
                overridingMembers.add(overrideProperty(classOrObject, selectedElement, lineDelimiter))
            }
        }
        return overridingMembers
    }

    private fun overrideFunction(classOrObject: KtClassOrObject,
                                 descriptor: FunctionDescriptor,
                                 lineDelimiter: String): KtNamedFunction {
        val newDescriptor: FunctionDescriptor = descriptor.copy(descriptor.getContainingDeclaration(), Modality.OPEN, descriptor.getVisibility(),
                descriptor.getKind(), /* copyOverrides = */ true)
        newDescriptor.setOverriddenDescriptors(listOf(descriptor))

        val returnType = descriptor.getReturnType()
        val returnsNotUnit = returnType != null && !KotlinBuiltIns.isUnit(returnType)
        val isAbstract = descriptor.getModality() == Modality.ABSTRACT

        val delegation = generateUnsupportedOrSuperCall(descriptor, functionBody)

        val body = "{$lineDelimiter" + (if (returnsNotUnit && !isAbstract) "return " else "") + delegation + "$lineDelimiter}"

        return KtPsiFactory(classOrObject.getProject()).createFunction(OVERRIDE_RENDERER.render(newDescriptor) + body)
    }

    private fun overrideProperty(classOrObject: KtClassOrObject,
                                 descriptor: PropertyDescriptor,
                                 lineDelimiter: String): KtElement {
        val newDescriptor = descriptor.copy(descriptor.getContainingDeclaration(), Modality.OPEN, descriptor.getVisibility(),
                descriptor.getKind(), /* copyOverrides = */ true) as PropertyDescriptor
        newDescriptor.setOverriddenDescriptors(listOf(descriptor))

        val body = StringBuilder()
        body.append("${lineDelimiter}get()")
        body.append(" = ")
        body.append(generateUnsupportedOrSuperCall(descriptor, propertyBody))
        if (descriptor.isVar()) {
            body.append("${lineDelimiter}set(value) {\n}")
        }
        return KtPsiFactory(classOrObject.getProject()).createProperty(OVERRIDE_RENDERER.render(newDescriptor) + body)
    }

    private fun generateUnsupportedOrSuperCall(descriptor: CallableMemberDescriptor,
                                               exception: String = functionBody): String {
        val isAbstract = descriptor.getModality() == Modality.ABSTRACT
        if (isAbstract) {
            return "$exception"
        } else {
            val builder = StringBuilder()
            builder.append("super.${descriptor.escapedName()}")

            if (descriptor is FunctionDescriptor) {
                val paramTexts = descriptor.getValueParameters().map {
                    val renderedName = it.escapedName()
                    if (it.varargElementType != null) "*$renderedName" else renderedName
                }
                paramTexts.joinTo(builder, prefix = "(", postfix = ")")
            }

            return builder.toString()
        }
    }

    private fun findInsertAfterAnchor(body: KtClassBody): PsiElement? {
        val afterAnchor = body.lBrace
        if (afterAnchor == null) return null

        val offset = getCaretOffset(editor)
        val offsetCursorElement = PsiTreeUtil.findFirstParent(body.getContainingFile().findElementAt(offset)) {
            it.getParent() == body
        }

        if (offsetCursorElement is PsiWhiteSpace) {
            return removeAfterOffset(offset, offsetCursorElement)
        }

        if (offsetCursorElement != null && offsetCursorElement != body.rBrace) {
            return offsetCursorElement
        }

        return afterAnchor
    }

    private fun removeAfterOffset(offset: Int, whiteSpace: PsiWhiteSpace): PsiElement {
        val spaceNode = whiteSpace.getNode()
        if (spaceNode.getTextRange().contains(offset)) {
            var beforeWhiteSpaceText = spaceNode.getText().substring(0, offset - spaceNode.getStartOffset())
            if (!StringUtil.containsLineBreak(beforeWhiteSpaceText)) {
                // Prevent insertion on same line
                beforeWhiteSpaceText += "\n"
            }

            val factory = KtPsiFactory(whiteSpace.getProject())

            val insertAfter = whiteSpace.getPrevSibling()
            whiteSpace.delete()

            val beforeSpace = factory.createWhiteSpace(beforeWhiteSpaceText)
            insertAfter.getParent().addAfter(beforeSpace, insertAfter)

            return insertAfter.getNextSibling()
        }

        return whiteSpace
    }

    public fun collectMethodsToGenerate(classOrObject: KtClassOrObject): Set<CallableMemberDescriptor> {
        val descriptor = classOrObject.resolveToDescriptor()
        if (descriptor is ClassDescriptor) {
            return OverrideResolver.getMissingImplementations(descriptor)
        }
        return emptySet()
    }


    fun DeclarationDescriptor.escapedName() = DescriptorRenderer.COMPACT.renderName(getName())
}