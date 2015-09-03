package org.jetbrains.kotlin.ui.editors

import org.jetbrains.kotlin.psi.JetVisitor
import org.jetbrains.kotlin.psi.JetNamedDeclaration
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.psi.JetCallableDeclaration
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.psi.JetParameter
import org.jetbrains.kotlin.psi.JetPrimaryConstructor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.psi.JetClassOrObject
import org.jetbrains.kotlin.psi.JetTreeVisitorVoid
import org.jetbrains.kotlin.psi.JetElement


private class KotlinSearchDeclarationVisitor(private val descriptor: DeclarationDescriptor): JetVisitor<JetNamedDeclaration?, Void?>() {
    override fun visitJetFile(file: JetFile, data: Void?): JetNamedDeclaration? = 
            visitJetElement(file, data)
    
    override fun visitJetElement(element: JetElement, data: Void?): JetNamedDeclaration? =
            element.getChildren().asSequence().map { 
                when(it) {
                    is JetElement -> it.accept(this, data)
                    else -> null
                }
             }.filterNotNull().firstOrNull()
    
    override fun visitNamedDeclaration(declaration: JetNamedDeclaration, data: Void?): JetNamedDeclaration? {
        if (compareDeclarations(declaration, descriptor)) {
            return declaration
        } else {
            return super.visitNamedDeclaration(declaration, data)
        }
    }
    
    override fun visitClassOrObject(classOrObject: JetClassOrObject, data: Void?): JetNamedDeclaration? {
        if (classOrObject.getPrimaryConstructor() == null && isConstructorForClass(classOrObject, descriptor)) {
            return classOrObject
        } else {
            return super.visitClassOrObject(classOrObject, data)
        }
    }
    
    override fun visitPrimaryConstructor(constructor: JetPrimaryConstructor, data: Void?): JetNamedDeclaration? {
        if (compareConstructors(constructor, descriptor)) {
            return constructor
        }
        return null
    }
    
    private fun isConstructorForClass(classOrObject: JetClassOrObject, possibleConstructor: DeclarationDescriptor) = 
            possibleConstructor is ConstructorDescriptor &&
            areFqNamesEqual(possibleConstructor.getContainingDeclaration(), classOrObject)
    
    private fun compareConstructors(declaration: JetPrimaryConstructor, descriptor: DeclarationDescriptor) =
            isConstructorForClass(declaration.getContainingClassOrObject(), descriptor)
    
    private fun compareDeclarations(declaration: JetNamedDeclaration, descriptor: DeclarationDescriptor): Boolean {
        if (!areFqNamesEqual(descriptor, declaration)) {
            return false
        }
        if (descriptor is CallableDescriptor) {
            if (declaration is JetCallableDeclaration) {
                return descriptor.getValueParameters().size() == declaration.getValueParameters().size()
            } else {
                return false
            }
        } else {
            return true
        }
    }
    
    private fun areFqNamesEqual(descriptor: DeclarationDescriptor, declaration: JetNamedDeclaration) = 
            DescriptorUtils.getFqName(descriptor) == declaration.getFqName()?.toUnsafe()
}

public fun findDeclarationInParsedFile(descriptor: DeclarationDescriptor, parsedFile: JetFile): Int {
    val visitor = KotlinSearchDeclarationVisitor(descriptor)
    return parsedFile.accept(visitor, null)?.getTextOffset() ?: 0
}