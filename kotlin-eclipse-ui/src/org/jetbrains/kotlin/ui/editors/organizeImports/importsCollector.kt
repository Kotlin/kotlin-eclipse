/*******************************************************************************
* Copyright 2000-2016 JetBrains s.r.o.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*
*******************************************************************************/
package org.jetbrains.kotlin.ui.editors.organizeImports

import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.psi.KtFile
import java.util.HashSet
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtImportList
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.scopes.HierarchicalScope
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.resolve.scopes.utils.findFunction
import org.jetbrains.kotlin.resolve.scopes.utils.findVariable
import org.jetbrains.kotlin.resolve.scopes.utils.findClassifier
import org.jetbrains.kotlin.ui.editors.codeassist.getResolutionScope
import org.jetbrains.kotlin.resolve.scopes.utils.replaceImportingScopes
import org.jetbrains.kotlin.resolve.scopes.utils.getImplicitReceiversHierarchy
import org.jetbrains.kotlin.resolve.scopes.utils.memberScopeAsImportingScope
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.core.references.createReferences
import org.jetbrains.kotlin.eclipse.ui.utils.getBindingContext
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.core.references.canBeResolvedViaImport
import org.jetbrains.kotlin.resolve.descriptorUtil.getImportableDescriptor

fun collectDescriptorsToImport(file: KtFile): Set<DeclarationDescriptor> {
    val visitor = CollectUsedDescriptorsVisitor(file)
    file.accept(visitor)
    return visitor.descriptors
}

private class CollectUsedDescriptorsVisitor(val file: KtFile) : KtVisitorVoid() {
    private val _descriptors = HashSet<DeclarationDescriptor>()
    private val currentPackageName = file.packageFqName
    
    private val bindingContext = getBindingContext(file)!!

    val descriptors: Set<DeclarationDescriptor>
        get() = _descriptors

    override fun visitElement(element: PsiElement) {
        element.acceptChildren(this)
    }

    override fun visitImportList(importList: KtImportList) {
    }

    override fun visitPackageDirective(directive: KtPackageDirective) {
    }
    
    override fun visitReferenceExpression(expression: KtReferenceExpression) {
        val references = createReferences(expression)
        for (reference in references) {
            val targets = bindingContext[BindingContext.SHORT_REFERENCE_TO_COMPANION_OBJECT, expression]
                    ?.let { listOf(it) }
                    ?: reference.getTargetDescriptors(bindingContext)
            
            val referencedName = (expression as? KtNameReferenceExpression)?.getReferencedNameAsName()
            
            for (target in targets) {
                val importableFqName = target.importableFqName ?: continue
                val parentFqName = importableFqName.parent()
                if (target is PackageViewDescriptor && parentFqName == FqName.ROOT) continue // no need to import top-level packages
                if (target !is PackageViewDescriptor && parentFqName == currentPackageName) continue

                if (!reference.canBeResolvedViaImport(target)) continue

                val importableDescriptor = target.getImportableDescriptor()

                if (referencedName != null && importableDescriptor.name != referencedName) continue // resolved via alias

                if (isAccessibleAsMember(importableDescriptor, expression, bindingContext)) continue

                _descriptors.add(importableDescriptor)
            }
        }
        
        super.visitReferenceExpression(expression)
    }

    private fun isAccessibleAsMember(target: DeclarationDescriptor, place: KtElement, bindingContext: BindingContext): Boolean {
        if (target.containingDeclaration !is ClassDescriptor) return false

        fun isInScope(scope: HierarchicalScope): Boolean {
            return when (target) {
                is FunctionDescriptor ->
                    scope.findFunction(target.name, NoLookupLocation.FROM_IDE) { it == target } != null

                is PropertyDescriptor ->
                    scope.findVariable(target.name, NoLookupLocation.FROM_IDE) { it == target } != null

                is ClassDescriptor ->
                    scope.findClassifier(target.name, NoLookupLocation.FROM_IDE) == target

                else -> false
            }
        }

        val resolutionScope = place.getResolutionScope(bindingContext)
        val noImportsScope = resolutionScope.replaceImportingScopes(null)

        if (isInScope(noImportsScope)) return true
        if (target !is ClassDescriptor) { // classes not accessible through receivers, only their constructors
            if (resolutionScope.getImplicitReceiversHierarchy().any { isInScope(it.type.memberScope.memberScopeAsImportingScope()) }) return true
        }
        return false
    }
}