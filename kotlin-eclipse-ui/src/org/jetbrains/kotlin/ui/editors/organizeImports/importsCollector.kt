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

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.core.references.canBeResolvedViaImport
import org.jetbrains.kotlin.core.references.createReferences
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.eclipse.ui.utils.getBindingContext
import org.jetbrains.kotlin.idea.imports.OptimizedImportsBuilder
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.getImportableDescriptor
import org.jetbrains.kotlin.resolve.scopes.HierarchicalScope
import org.jetbrains.kotlin.resolve.scopes.utils.*
import org.jetbrains.kotlin.ui.editors.codeassist.getResolutionScope
import java.util.HashSet
import kotlin.collections.LinkedHashMap
import kotlin.collections.LinkedHashSet

fun collectDescriptorsToImport(file: KtFile): OptimizedImportsBuilder.InputData {
    val visitor = CollectUsedDescriptorsVisitor(file)
    file.accept(visitor)
    return OptimizedImportsBuilder.InputData(visitor.descriptorsToImport, visitor.namesToImport, emptyList(), emptySet() /*TODO??*/)
}

private class CollectUsedDescriptorsVisitor(val file: KtFile) : KtVisitorVoid() {
    private val currentPackageName = file.packageFqName

    val descriptorsToImport = LinkedHashSet<DeclarationDescriptor>()
    val namesToImport = LinkedHashMap<FqName, HashSet<Name>>()

    private val bindingContext = getBindingContext(file)!!

    private val aliases: Map<FqName, List<Name>> = file.importDirectives
        .asSequence()
        .filter { !it.isAllUnder && it.alias != null }
        .mapNotNull { it.importPath }
        .groupBy(keySelector = { it.fqName }, valueTransform = { it.importedName as Name })

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

            val names = reference.resolvesByNames

            val targets = bindingContext[BindingContext.SHORT_REFERENCE_TO_COMPANION_OBJECT, expression]
                    ?.let { listOf(it) }
                    ?: reference.getTargetDescriptors(bindingContext)

            val referencedName = (expression as? KtNameReferenceExpression)?.getReferencedNameAsName()

            for (target in targets) {
                val importableFqName = target.importableFqName ?: continue
                val parentFqName = importableFqName.parent()
                if (target is PackageViewDescriptor && parentFqName == FqName.ROOT) continue // no need to import top-level packages
                if (target !is PackageViewDescriptor && parentFqName == currentPackageName && (importableFqName !in aliases)) continue

                if (!reference.canBeResolvedViaImport(target)) continue

                val importableDescriptor = target.getImportableDescriptor()

                if (isAccessibleAsMember(importableDescriptor, expression, bindingContext)) continue

                val descriptorNames = (aliases[importableFqName].orEmpty() + importableFqName.shortName()).intersect(names)
                namesToImport.getOrPut(importableFqName) { LinkedHashSet() } += descriptorNames
                descriptorsToImport += importableDescriptor
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