package org.jetbrains.kotlin.ui.editors.codeassist

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension

enum class CompletionElementType {
	KFUNCTION_TOP,
	KVARIABLE_TOP,
	KFUNCTION_EXT,
	KVARIABLE_EXT,
	KFUNCTION,
	KVARIABLE,
	KCLASS_OBJECT,
	UNKNOWN;
	
	companion object {
		fun from(descriptor: DeclarationDescriptor) : CompletionElementType {
	        return when(descriptor) {
	            is ClassDescriptor, is TypeParameterDescriptor, is TypeAliasDescriptor -> KCLASS_OBJECT
	            is FunctionDescriptor -> when {
					descriptor.isExtension -> KFUNCTION_EXT
					descriptor.isTopLevelInPackage() -> KFUNCTION_TOP
					else -> KFUNCTION
				}
	            is VariableDescriptor -> when {
					descriptor.isExtension -> KVARIABLE_EXT
					descriptor.isTopLevelInPackage() -> KVARIABLE_TOP
					else -> KVARIABLE
				}
	            else -> UNKNOWN
			}
		}
	}
}