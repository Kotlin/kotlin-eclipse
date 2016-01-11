package org.jetbrains.kotlin.ui.editors.hover

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithSource
import org.jetbrains.kotlin.resolve.source.PsiSourceElement
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.kdoc.parser.KDocKnownTag
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

fun findKDoc(declaration: DeclarationDescriptor, ktDeclaration: KtDeclaration): KDocTag? {
        // KDoc for primary constructor is located inside of its class KDoc
    var psiDeclaration = ktDeclaration
    if (psiDeclaration is KtPrimaryConstructor) {
        psiDeclaration = psiDeclaration.getContainingClassOrObject()
    }

    if (psiDeclaration is KtDeclaration) {
        val kdoc = psiDeclaration.getDocComment()
        if (kdoc != null) {
            if (declaration is ConstructorDescriptor) {
                // ConstructorDescriptor resolves to the same JetDeclaration
                val constructorSection = kdoc.findSectionByTag(KDocKnownTag.CONSTRUCTOR)
                if (constructorSection != null) {
                    return constructorSection
                }
            }
            return kdoc.getDefaultSection()
        }
    }

    if (declaration is PropertyDescriptor) {
        val containingClassDescriptor = declaration.getContainingDeclaration() as? ClassDescriptor
        if (containingClassDescriptor != null) {
            val classKDoc = findKDoc(containingClassDescriptor, ktDeclaration)?.getParentOfType<KDoc>(false)
            if (classKDoc != null) {
                val propertySection = classKDoc.findSectionByTag(KDocKnownTag.PROPERTY,
                                                                 declaration.getName().asString())
                if (propertySection != null) {
                    return propertySection
                }
            }
        }
    }

    if (declaration is CallableDescriptor) {
        for (baseDescriptor in declaration.getOverriddenDescriptors()) {
            val baseKDoc = findKDoc(baseDescriptor.getOriginal(), ktDeclaration)
            if (baseKDoc != null) {
                return baseKDoc
            }
        }
    }

    return null
}

inline public fun <reified T : PsiElement> PsiElement.getParentOfType(strict: Boolean): T? {
    return PsiTreeUtil.getParentOfType(this, T::class.java, strict)
}