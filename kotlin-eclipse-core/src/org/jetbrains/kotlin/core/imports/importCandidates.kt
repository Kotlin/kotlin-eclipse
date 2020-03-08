package org.jetbrains.kotlin.core.imports

import org.eclipse.jdt.core.search.TypeNameMatch
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.imports.importableFqName

sealed class ImportCandidate {
    abstract val fullyQualifiedName: String?
    abstract val packageName: String?
    abstract val simpleName: String
}

class TypeCandidate(val match: TypeNameMatch) : ImportCandidate() {
    override val fullyQualifiedName: String? = match.fullyQualifiedName
    override val packageName: String? = match.packageName
    override val simpleName: String = match.simpleTypeName
}

class FunctionCandidate(val descriptor: CallableDescriptor) : ImportCandidate() {
    override val fullyQualifiedName: String? = descriptor.importableFqName?.asString()

    override val packageName: String? =
        descriptor.importableFqName
            ?.takeUnless { it.isRoot }
            ?.parent()
            ?.asString()

    override val simpleName: String = descriptor.name.asString()
}

data class UniqueAndAmbiguousImports(
    val uniqueImports: List<ImportCandidate>,
    val ambiguousImports: List<List<ImportCandidate>>
)
