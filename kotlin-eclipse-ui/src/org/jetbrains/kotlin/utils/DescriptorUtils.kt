package org.jetbrains.kotlin.utils

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.MemberScope.Companion.ALL_NAME_FILTER

private fun ModuleDescriptor.findCurrentDescriptorForMember(originalDescriptor: MemberDescriptor): DeclarationDescriptor? {
    val containingDeclaration = findCurrentDescriptor(originalDescriptor.containingDeclaration)
    val memberScope = containingDeclaration?.memberScope ?: return null

    val renderedOriginal: String = DescriptorRenderer.FQ_NAMES_IN_TYPES.render(originalDescriptor)
    val descriptors: Collection<DeclarationDescriptor> =
        if (originalDescriptor is ConstructorDescriptor && containingDeclaration is ClassDescriptor) {
        containingDeclaration.constructors
    } else {
        memberScope.getContributedDescriptors(DescriptorKindFilter.ALL, ALL_NAME_FILTER)
    }
    for (member in descriptors) {
        if (renderedOriginal == DescriptorRenderer.FQ_NAMES_IN_TYPES.render(member)) {
            return member
        }
    }
    return null
}

fun ModuleDescriptor.findCurrentDescriptor(originalDescriptor: DeclarationDescriptor): DeclarationDescriptor? {
    if (originalDescriptor is ClassDescriptor) {
        val classId: ClassId = originalDescriptor.classId ?: return null
        return findClassAcrossModuleDependencies(classId)
    }
    if (originalDescriptor is PackageFragmentDescriptor) {
        return getPackage(originalDescriptor.fqName)
    }
    return if (originalDescriptor is MemberDescriptor) {
        findCurrentDescriptorForMember(originalDescriptor)
    } else null
}

private val DeclarationDescriptor.memberScope: MemberScope? get() = when (this) {
    is ClassDescriptor -> defaultType.memberScope
    is PackageFragmentDescriptor -> getMemberScope()
    else -> null
}
