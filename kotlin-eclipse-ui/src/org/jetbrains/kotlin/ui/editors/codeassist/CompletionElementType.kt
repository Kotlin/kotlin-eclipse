package org.jetbrains.kotlin.eclipse.ui.utils

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.eclipse.jdt.core.Flags

enum class CompletionElementType {
	KFUNCTION,
	KVARIABLE,
	KCLASS_OBJECT,
	UNKNOWN;
	
	companion object {
		fun from(descriptor: DeclarationDescriptor) : CompletionElementType {
	        return when(descriptor) {
	            is ClassDescriptor, is TypeParameterDescriptor, is TypeAliasDescriptor -> KCLASS_OBJECT
	            is FunctionDescriptor -> KFUNCTION
	            is VariableDescriptor -> KVARIABLE
	            else -> UNKNOWN
			}
		}
	}
}