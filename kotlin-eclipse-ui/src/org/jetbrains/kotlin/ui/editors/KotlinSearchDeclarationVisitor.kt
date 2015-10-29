package org.jetbrains.kotlin.ui.editors

import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement


private class KotlinSearchDeclarationVisitor(private val descriptor: DeclarationDescriptor): KtVisitor<KtNamedDeclaration?, Void?>() {
    override fun visitJetFile(file: KtFile, data: Void?): KtNamedDeclaration? = 
            visitJetElement(file, data)
    
    override fun visitJetElement(element: KtElement, data: Void?): KtNamedDeclaration? =
            element.getChildren().asSequence().map { 
                when(it) {
                    is KtElement -> it.accept(this, data)
                    else -> null
                }
             }.filterNotNull().firstOrNull()
    
    override fun visitNamedDeclaration(declaration: KtNamedDeclaration, data: Void?): KtNamedDeclaration? {
        if (compareDeclarations(declaration, descriptor)) {
            return declaration
        } else {
            return super.visitNamedDeclaration(declaration, data)
        }
    }
    
    override fun visitClassOrObject(classOrObject: KtClassOrObject, data: Void?): KtNamedDeclaration? {
        if (classOrObject.getPrimaryConstructor() == null && isConstructorForClass(classOrObject, descriptor)) {
            return classOrObject
        } else {
            return super.visitClassOrObject(classOrObject, data)
        }
    }
    
    override fun visitPrimaryConstructor(constructor: KtPrimaryConstructor, data: Void?): KtNamedDeclaration? {
        if (compareConstructors(constructor, descriptor)) {
            return constructor
        }
        return null
    }
    
    private fun isConstructorForClass(classOrObject: KtClassOrObject, possibleConstructor: DeclarationDescriptor) = 
            possibleConstructor is ConstructorDescriptor &&
            areFqNamesEqual(possibleConstructor.getContainingDeclaration(), classOrObject)
    
    private fun compareConstructors(declaration: KtPrimaryConstructor, descriptor: DeclarationDescriptor) =
            isConstructorForClass(declaration.getContainingClassOrObject(), descriptor)
    
    private fun compareDeclarations(declaration: KtNamedDeclaration, descriptor: DeclarationDescriptor): Boolean {
        if (!areFqNamesEqual(descriptor, declaration)) {
            return false
        }
        if (descriptor is CallableDescriptor) {
            if (declaration is KtCallableDeclaration) {
                return descriptor.getValueParameters().size() == declaration.getValueParameters().size()
            } else {
                return false
            }
        } else {
            return true
        }
    }
    
    private fun areFqNamesEqual(descriptor: DeclarationDescriptor, declaration: KtNamedDeclaration) = 
            DescriptorUtils.getFqName(descriptor) == declaration.getFqName()?.toUnsafe()
}

public fun findDeclarationInParsedFile(descriptor: DeclarationDescriptor, parsedFile: KtFile): Int {
    val visitor = KotlinSearchDeclarationVisitor(descriptor)
    return parsedFile.accept(visitor, null)?.getTextOffset() ?: 0
}