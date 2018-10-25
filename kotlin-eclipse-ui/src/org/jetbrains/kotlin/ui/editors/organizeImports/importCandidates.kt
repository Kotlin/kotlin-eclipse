package org.jetbrains.kotlin.ui.editors.organizeImports

import org.eclipse.jdt.core.search.TypeNameMatch
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.imports.importableFqName

sealed class ImportCandidate {
    abstract val fullyQualifiedName: String?
}

class TypeCandidate(val match: TypeNameMatch): ImportCandidate() {
    override val fullyQualifiedName: String?
        get() = match.fullyQualifiedName
}

class FunctionCandidate(val descriptor: CallableDescriptor): ImportCandidate() {
    override val fullyQualifiedName: String?
        get() = descriptor.importableFqName?.asString()
}

data class UniqueAndAmbiguousImports(
    val uniqueImports: List<ImportCandidate>,
    val ambiguousImports: List<List<ImportCandidate>>)
