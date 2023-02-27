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
package org.jetbrains.kotlin.core.utils

import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageFeature.DefaultImportOfPackageKotlinComparisons
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.util.ImportDescriptorResult
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices

class KotlinImportInserterHelper : ImportInsertHelper() {
    private val importSortComparator: Comparator<ImportPath> = Comparator { _, _ -> 0 }

    override fun getImportSortComparator(contextFile: KtFile): Comparator<ImportPath> {
        return importSortComparator
    }

    override fun importDescriptor(element: KtElement, descriptor: DeclarationDescriptor, runImmediately: Boolean, forceAllUnderImport: Boolean, aliasName: Name?): ImportDescriptorResult {
        throw UnsupportedOperationException()
    }

    override fun importPsiClass(element: KtElement, psiClass: PsiClass, runImmediately: Boolean): ImportDescriptorResult {
        throw UnsupportedOperationException()
    }

    override fun isImportedWithDefault(importPath: ImportPath, contextFile: KtFile): Boolean {
        val defaultImports = JvmPlatformAnalyzerServices.getDefaultImports(
            if (LanguageVersionSettingsImpl.DEFAULT.supportsFeature(DefaultImportOfPackageKotlinComparisons)) LanguageVersionSettingsImpl.DEFAULT
            else LanguageVersionSettingsImpl(LanguageVersion.KOTLIN_1_0, ApiVersion.KOTLIN_1_0),
            true
        )
        return importPath.isImported(defaultImports)
    }

    override fun mayImportOnShortenReferences(descriptor: DeclarationDescriptor, contextFile: KtFile): Boolean {
        return false
    }

    override fun isImportedWithLowPriorityDefaultImport(importPath: ImportPath, contextFile: KtFile): Boolean =
        isImportedWithDefault(importPath, contextFile)
}


// TODO: obtain these functions from fqNameUtil.kt (org.jetbrains.kotlin.idea.refactoring.fqName)
fun FqName.isImported(importPath: ImportPath, skipAliasedImports: Boolean = true): Boolean {
    return when {
        skipAliasedImports && importPath.hasAlias() -> false
        importPath.isAllUnder && !isRoot -> importPath.fqName == this.parent()
        else -> importPath.fqName == this
    }
}

fun ImportPath.isImported(alreadyImported: ImportPath): Boolean {
    return if (isAllUnder || hasAlias()) this == alreadyImported else fqName.isImported(alreadyImported)
}

fun ImportPath.isImported(imports: Iterable<ImportPath>): Boolean = imports.any { isImported(it) }