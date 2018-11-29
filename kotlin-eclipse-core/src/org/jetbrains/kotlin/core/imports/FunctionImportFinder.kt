package org.jetbrains.kotlin.core.imports

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.DeclarationDescriptorVisitorEmptyBodies
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.scopes.MemberScope

class FunctionImportFinder(
    val filter: (CallableDescriptor) -> Boolean
) : DeclarationDescriptorVisitorEmptyBodies<List<CallableDescriptor>, List<String>>() {
    override fun visitModuleDeclaration(
        descriptor: ModuleDescriptor,
        data: List<String>
    ): List<CallableDescriptor> = descriptor.getPackage(FqName.ROOT).accept(this, data)

    override fun visitPackageViewDescriptor(
        descriptor: PackageViewDescriptor,
        data: List<String>
    ): List<CallableDescriptor> = visitMemberScope(descriptor.memberScope, data)

    override fun visitFunctionDescriptor(
        descriptor: FunctionDescriptor,
        data: List<String>
    ): List<CallableDescriptor> = visitMember(descriptor)

    override fun visitPropertyDescriptor(
        descriptor: PropertyDescriptor,
        data: List<String>
    ): List<CallableDescriptor> = visitMember(descriptor)

    private fun visitMemberScope(
        scope: MemberScope,
        data: List<String>
    ): List<CallableDescriptor> {
        val possiblePackageLevelFunctions =
            data.filter { it.substringBefore('.', "").endsWith("Kt") }
                .map { it.substringAfter('.') }

        val groupedEntries = (data + possiblePackageLevelFunctions).groupByQualifier()
        return scope.getContributedDescriptors { it.asString() in groupedEntries.keys }
            .flatMap { it.accept(this, groupedEntries[it.name.asString()].orEmpty()) }
    }

    private fun visitMember(descriptor: CallableDescriptor): List<CallableDescriptor> =
        if (filter(descriptor)) listOf(descriptor) else emptyList()


    override fun visitDeclarationDescriptor(
        descriptor: DeclarationDescriptor,
        data: List<String>
    ): List<FunctionDescriptor> =
        emptyList()
}

private fun Iterable<String>.groupByQualifier() = groupBy(
    keySelector = { it.substringBefore('.') },
    valueTransform = { it.substringAfter('.', "") }
)