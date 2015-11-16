package org.jetbrains.kotlin.ui.editors.quickassist

import com.intellij.psi.PsiElement
import org.eclipse.jface.text.IDocument
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer
import org.eclipse.jdt.core.JavaCore
import org.jetbrains.kotlin.resolve.BindingContextUtils
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.resolve.OverrideResolver
import com.intellij.psi.util.PsiTreeUtil
import org.eclipse.jface.dialogs.MessageDialog
import java.util.ArrayList
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtClassBody
import com.intellij.psi.PsiWhiteSpace
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.NameShortness
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.eclipse.jface.text.TextUtilities
import org.jetbrains.kotlin.ui.formatter.AlignmentStrategy
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.jetbrains.kotlin.eclipse.ui.utils.IndenterUtil
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.renderer.DescriptorRendererModifier
import org.jetbrains.kotlin.renderer.OverrideRenderingPolicy
import org.jetbrains.kotlin.core.builder.KotlinPsiManager

public class KotlinImplementMethodsProposal : KotlinQuickAssistProposal() {
    private val OVERRIDE_RENDERER = DescriptorRenderer.withOptions {
            renderDefaultValues = false
            modifiers = setOf(DescriptorRendererModifier.OVERRIDE)
            withDefinedIn = false
            nameShortness = NameShortness.SHORT
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
		val editor = getActiveEditor()!!
		val psiFactory = KtPsiFactory(classOrObject.getProject())
        if (body == null) {
            val bodyText = "${psiFactory.createWhiteSpace().getText()}${psiFactory.createEmptyClassBody().getText()}"
            insertAfter(classOrObject, bodyText)
        } else {
            removeWhitespaceAfterLBrace(body, editor.getViewer().getDocument(), editor)
        }

		val insertOffset = findLBraceEndOffset(editor.getViewer().getDocument(), getStartOffset(classOrObject, editor))
        if (insertOffset == null) return
        
        val generatedText = StringBuilder()
        val lineDelimiter = TextUtilities.getDefaultLineDelimiter(editor.getViewer().getDocument())
        val indent = AlignmentStrategy.computeIndent(classOrObject.getNode()) + 1
        
        val newLine = psiFactory.createNewLine()
        
        val newLineWithShift = AlignmentStrategy.alignCode(newLine.getNode(), indent, lineDelimiter)
        
        val generatedMembers = generateOverridingMembers(selectedElements, classOrObject)
        for (i in generatedMembers.indices) {
            generatedText.append(newLineWithShift)
            generatedText.append(AlignmentStrategy.alignCode(generatedMembers[i].getNode(), indent, lineDelimiter))
            if (i != generatedMembers.lastIndex) {
            	generatedText.append(newLineWithShift)
            }
        }
		generatedText.append(AlignmentStrategy.alignCode(newLine.getNode(), indent - 1, lineDelimiter))
        
        document.replace(insertOffset, 0, generatedText.toString())
	}
    
    private fun removeWhitespaceAfterLBrace(body: KtClassBody, document: IDocument, editor: KotlinFileEditor) {
        val lBrace = body.lBrace
        if (lBrace != null) {
            val sibling = lBrace.getNextSibling()
            val needNewLine = sibling.getNextSibling() is KtDeclaration
            if (sibling is PsiWhiteSpace && !needNewLine) {
            	document.replace(getStartOffset(sibling, editor), sibling.getTextLength(), "")
            }
        }
    }
    
    private fun findLBraceEndOffset(document: IDocument, startIndex: Int) : Int? {	
        val text = document.get()
        for (i in startIndex..text.lastIndex) {
            if (text[i] == '{') return i + 1
        }
        
        return null
    }
	
    private fun generateOverridingMembers(selectedElements: Set<CallableMemberDescriptor>,
                                          classOrObject: KtClassOrObject): List<KtElement> {
        val overridingMembers = ArrayList<KtElement>()
        for (selectedElement in selectedElements) {
            if (selectedElement is SimpleFunctionDescriptor) {
                overridingMembers.add(overrideFunction(classOrObject, selectedElement))
            } else if (selectedElement is PropertyDescriptor) {
                overridingMembers.add(overrideProperty(classOrObject, selectedElement))
            }
        }
        return overridingMembers
    }

    private fun overrideFunction(classOrObject: KtClassOrObject, descriptor: FunctionDescriptor): KtNamedFunction {
        val newDescriptor = descriptor.copy(descriptor.getContainingDeclaration(), Modality.OPEN, descriptor.getVisibility(),
                                            descriptor.getKind(), /* copyOverrides = */ true)
        newDescriptor.addOverriddenDescriptor(descriptor)

        val returnType = descriptor.getReturnType()
        val returnsNotUnit = returnType != null && !KotlinBuiltIns.isUnit(returnType)
        val isAbstract = descriptor.getModality() == Modality.ABSTRACT

        val delegation = generateUnsupportedOrSuperCall(classOrObject, descriptor)

        val body = "{\n" + (if (returnsNotUnit && !isAbstract) "return " else "") + delegation + "\n}"

        return KtPsiFactory(classOrObject.getProject()).createFunction(OVERRIDE_RENDERER.render(newDescriptor) + body)
    }
	
    private fun overrideProperty(classOrObject: KtClassOrObject, descriptor: PropertyDescriptor): KtElement {
        val newDescriptor = descriptor.copy(descriptor.getContainingDeclaration(), Modality.OPEN, descriptor.getVisibility(),
                                            descriptor.getKind(), /* copyOverrides = */ true) as PropertyDescriptor
        newDescriptor.addOverriddenDescriptor(descriptor)

        val body = StringBuilder()
        body.append("\nget()")
        body.append(" = ")
        body.append(generateUnsupportedOrSuperCall(classOrObject, descriptor))
        if (descriptor.isVar()) {
            body.append("\nset(value) {\n}")
        }
        return KtPsiFactory(classOrObject.getProject()).createProperty(OVERRIDE_RENDERER.render(newDescriptor) + body)
    }

	
    private fun generateUnsupportedOrSuperCall(classOrObject: KtClassOrObject, descriptor: CallableMemberDescriptor): String {
        val isAbstract = descriptor.getModality() == Modality.ABSTRACT
        if (isAbstract) {
            return "throw UnsupportedOperationException()"
        }
        else {
            val builder = StringBuilder()
            builder.append("super")
            if (classOrObject.getDelegationSpecifiers().size > 1) {
                builder.append("<").append(descriptor.getContainingDeclaration().escapedName()).append(">")
            }
            builder.append(".").append(descriptor.escapedName())

            if (descriptor is FunctionDescriptor) {
                val paramTexts = descriptor.getValueParameters().map {
                    val renderedName = it.escapedName()
                    if (it.varargElementType != null) "*$renderedName" else renderedName
                }
                paramTexts.joinTo(builder, prefix="(", postfix=")")
            }

            return builder.toString()
        }
    }

	
    private fun findInsertAfterAnchor(body: KtClassBody): PsiElement? {
        val afterAnchor = body.lBrace
        if (afterAnchor == null) return null

        val offset = getCaretOffset(getActiveEditor()!!)
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