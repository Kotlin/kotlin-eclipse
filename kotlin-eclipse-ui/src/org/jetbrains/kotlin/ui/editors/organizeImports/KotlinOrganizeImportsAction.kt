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

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds
import org.eclipse.jdt.internal.ui.actions.ActionMessages
import org.eclipse.jdt.internal.ui.dialogs.MultiElementListSelectionDialog
import org.eclipse.jdt.internal.ui.util.TypeNameMatchLabelProvider
import org.eclipse.jdt.ui.JavaUI
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds
import org.eclipse.jdt.ui.actions.SelectionDispatchAction
import org.eclipse.jface.viewers.LabelProvider
import org.eclipse.jface.window.Window
import org.eclipse.swt.graphics.Image
import org.eclipse.ui.ISharedImages
import org.eclipse.ui.PlatformUI
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.core.formatting.codeStyle
import org.jetbrains.kotlin.core.imports.*
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.core.model.KotlinEnvironment
import org.jetbrains.kotlin.core.preferences.languageVersionSettings
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer
import org.jetbrains.kotlin.core.resolve.KotlinResolutionFacade
import org.jetbrains.kotlin.eclipse.ui.utils.getBindingContext
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings
import org.jetbrains.kotlin.idea.core.formatter.KotlinPackageEntry
import org.jetbrains.kotlin.idea.core.formatter.KotlinPackageEntry.Companion.ALL_OTHER_ALIAS_IMPORTS_ENTRY
import org.jetbrains.kotlin.idea.core.formatter.KotlinPackageEntryTable
import org.jetbrains.kotlin.idea.formatter.kotlinCustomSettings
import org.jetbrains.kotlin.idea.imports.OptimizedImportsBuilder
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices
import org.jetbrains.kotlin.ui.editors.KotlinCommonEditor
import org.jetbrains.kotlin.ui.editors.quickfix.placeImports
import org.jetbrains.kotlin.ui.editors.quickfix.replaceImports


class KotlinOrganizeImportsAction(private val editor: KotlinCommonEditor) : SelectionDispatchAction(editor.site) {
    init {
        actionDefinitionId = IJavaEditorActionDefinitionIds.ORGANIZE_IMPORTS

        text = ActionMessages.OrganizeImportsAction_label
        toolTipText = ActionMessages.OrganizeImportsAction_tooltip
        description = ActionMessages.OrganizeImportsAction_description

        PlatformUI.getWorkbench().helpSystem.setHelp(this, IJavaHelpContextIds.ORGANIZE_IMPORTS_ACTION)
    }

    companion object {
        const val ACTION_ID = "OrganizeImports"
    }

    override fun run() {
        val bindingContext = editor.getBindingContext() ?: return
        val ktFile = editor.parsedFile ?: return
        val file = editor.eclipseFile ?: return
        val (result, container) = KotlinAnalyzer.analyzeFile(ktFile)
        val resolutionFacade = container?.let { KotlinResolutionFacade(file, it, result.moduleDescriptor) } ?: return
        val environment = KotlinEnvironment.getEnvironment(file.project)

        val languageVersionSettings = environment.compilerProperties.languageVersionSettings
        val candidatesFilter = DefaultImportPredicate(JvmPlatformAnalyzerServices, languageVersionSettings)

        val referencesToImport = bindingContext.diagnostics
            .filter { it.factory in FIXABLE_DIAGNOSTICS && it.psiFile == ktFile }
            .map { it.psiElement }
            .distinctBy { it.text }

        val (uniqueImports, ambiguousImports) = findImportCandidates(
            referencesToImport,
            bindingContext,
            resolutionFacade,
            candidatesFilter
        )

        val allRequiredImports = uniqueImports.toMutableList()
        if (ambiguousImports.isNotEmpty()) {
            val chosenImports = chooseImports(ambiguousImports) ?: return

            allRequiredImports.addAll(chosenImports)
        }

        placeImports(allRequiredImports, file, editor.document)

        KotlinPsiManager.commitFile(file, editor.document)

        optimizeImports(languageVersionSettings)
    }

    private fun optimizeImports(languageVersionSettings: LanguageVersionSettings) {
        val ktFile = editor.parsedFile ?: return
        val file = editor.eclipseFile ?: return
        val importsData = collectDescriptorsToImport(ktFile)
        val kotlinCodeStyleSettings = file.project.codeStyle.kotlinCustomSettings

        val optimizedImports = prepareOptimizedImports(ktFile, importsData, kotlinCodeStyleSettings, languageVersionSettings) ?: return

        replaceImports(
            optimizedImports.sortedWith(ImportPathComparator(kotlinCodeStyleSettings.PACKAGES_IMPORT_LAYOUT))
                .map { it.toString() }, file, editor.document
        )
    }

    // null signalizes about cancelling operation
    private fun chooseImports(ambiguousImports: List<List<ImportCandidate>>): List<ImportCandidate>? {
        val dialog = MultiElementListSelectionDialog(shell, ImportCandidatesLabelProvider)
        dialog.setTitle(ActionMessages.OrganizeImportsAction_selectiondialog_title)
        dialog.setMessage(ActionMessages.OrganizeImportsAction_selectiondialog_message)

        val arrayImports = Array(ambiguousImports.size) { i ->
            Array(ambiguousImports[i].size) { j ->
                ambiguousImports[i][j]
            }
        }

        dialog.setElements(arrayImports)

        return if (dialog.open() == Window.OK) {
            dialog.result.mapNotNull { (it as Array<*>).firstOrNull() as? ImportCandidate }
        } else {
            null
        }
    }
}

fun prepareOptimizedImports(
    file: KtFile,
    importsData: OptimizedImportsBuilder.InputData,
    settings: KotlinCodeStyleSettings,
    languageVersionSettings: LanguageVersionSettings
): List<ImportPath>? =
    OptimizedImportsBuilder(
        file,
        importsData,
        OptimizedImportsBuilder.Options(
            settings.NAME_COUNT_TO_USE_STAR_IMPORT,
            settings.NAME_COUNT_TO_USE_STAR_IMPORT_FOR_MEMBERS
        ) { fqName -> fqName.asString() in settings.PACKAGES_TO_USE_STAR_IMPORTS },
        languageVersionSettings.apiVersion
    ).buildOptimizedImports()

object ImportCandidatesLabelProvider : LabelProvider() {
    private val typeLabelProvider = TypeNameMatchLabelProvider(TypeNameMatchLabelProvider.SHOW_FULLYQUALIFIED)

    override fun getImage(element: Any?): Image? = when (element) {
        is TypeCandidate -> typeLabelProvider.getImage(element.match)
        is FunctionCandidate -> JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT)
        else -> null
    }

    override fun getText(element: Any?): String? = when (element) {
        is TypeCandidate -> typeLabelProvider.getText(element.match)
        is FunctionCandidate -> element.fullyQualifiedName
        else -> element.toString()
            .also { KotlinLogger.logWarning("Unknown type of import candidate: $element") }
    }
}

internal class ImportPathComparator(private val packageTable: KotlinPackageEntryTable) : Comparator<ImportPath> {

    override fun compare(import1: ImportPath, import2: ImportPath): Int {
        val ignoreAlias = import1.hasAlias() && import2.hasAlias()

        return compareValuesBy(
            import1,
            import2,
            { import -> bestEntryMatchIndex(import, ignoreAlias) },
            { import -> import.toString() }
        )
    }

    private fun bestEntryMatchIndex(path: ImportPath, ignoreAlias: Boolean): Int {
        var bestEntryMatch: KotlinPackageEntry? = null
        var bestIndex: Int = -1

        for ((index, entry) in packageTable.getEntries().withIndex()) {
            if (entry.isBetterMatchForPackageThan(bestEntryMatch, path, ignoreAlias)) {
                bestEntryMatch = entry
                bestIndex = index
            }
        }

        return bestIndex
    }
}

private fun KotlinPackageEntry.isBetterMatchForPackageThan(entry: KotlinPackageEntry?, path: ImportPath, ignoreAlias: Boolean): Boolean {
    if (!matchesImportPath(path, ignoreAlias)) return false
    if (entry == null) return true

    // Any matched package is better than ALL_OTHER_IMPORTS_ENTRY
    if (this == KotlinPackageEntry.ALL_OTHER_IMPORTS_ENTRY) return false
    if (entry == KotlinPackageEntry.ALL_OTHER_IMPORTS_ENTRY) return true

    if (entry.withSubpackages != withSubpackages) return !withSubpackages

    return entry.packageName.count { it == '.' } < packageName.count { it == '.' }
}

/**
 * In current implementation we assume that aliased import can be matched only by
 * [ALL_OTHER_ALIAS_IMPORTS_ENTRY] which is always present.
 */
private fun KotlinPackageEntry.matchesImportPath(importPath: ImportPath, ignoreAlias: Boolean): Boolean {
    if (!ignoreAlias && importPath.hasAlias()) {
        return this == ALL_OTHER_ALIAS_IMPORTS_ENTRY
    }

    if (this == KotlinPackageEntry.ALL_OTHER_IMPORTS_ENTRY) return true

    return matchesPackageName(importPath.pathStr)
}