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

import org.eclipse.jdt.ui.actions.SelectionDispatchAction
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds
import org.eclipse.jdt.internal.ui.actions.ActionMessages
import org.eclipse.ui.PlatformUI
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds
import org.jetbrains.kotlin.eclipse.ui.utils.getBindingContext
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.ui.editors.quickfix.findApplicableTypes
import org.jetbrains.kotlin.ui.editors.quickfix.placeImports
import org.jetbrains.kotlin.ui.editors.quickfix.replaceImports
import org.eclipse.jdt.core.IType
import org.eclipse.jdt.core.search.TypeNameMatch
import org.eclipse.jdt.internal.ui.util.TypeNameMatchLabelProvider
import org.eclipse.jdt.internal.ui.dialogs.MultiElementListSelectionDialog
import org.eclipse.jface.window.Window
import java.util.ArrayList
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.idea.imports.prepareOptimizedImports
import org.jetbrains.kotlin.core.builder.KotlinPsiManager

class KotlinOrganizeImportsAction(private val editor: KotlinFileEditor) : SelectionDispatchAction(editor.site) {
    init {
        setActionDefinitionId(IJavaEditorActionDefinitionIds.ORGANIZE_IMPORTS)
        
        setText(ActionMessages.OrganizeImportsAction_label);
        setToolTipText(ActionMessages.OrganizeImportsAction_tooltip);
        setDescription(ActionMessages.OrganizeImportsAction_description);

        PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.ORGANIZE_IMPORTS_ACTION);
    }
    
    companion object {
        val ACTION_ID = "OrganizeImports"
    }
    
    override fun run() {
        val bindingContext = getBindingContext(editor) ?: return
        val ktFile = editor.parsedFile ?: return
        val file = editor.getFile() ?: return
        
        val typeNamesToImport = bindingContext.diagnostics
                .filter { it.factory == Errors.UNRESOLVED_REFERENCE && it.psiFile == ktFile }
                .map { it.psiElement.text }
                .distinct()
        
        val (uniqueImports, ambiguousImports) = findTypesToImport(typeNamesToImport)
        
        val allRequiredImports = ArrayList(uniqueImports)
        if (ambiguousImports.isNotEmpty()) {
            val chosenImports = chooseImports(ambiguousImports)
            if (chosenImports == null) return
            
            allRequiredImports.addAll(chosenImports)
        }
        
        placeImports(allRequiredImports, file, editor.document)
        
        KotlinPsiManager.commitFile(file, editor.document)
        
        optimizeImports()
    }
    
    private fun optimizeImports() {
        val ktFile = editor.parsedFile ?: return
        val file = editor.getFile() ?: return
        val descriptorsToImport = collectDescriptorsToImport(ktFile)
        
        val optimizedImports = prepareOptimizedImports(ktFile, descriptorsToImport) ?: return
        
        replaceImports(optimizedImports.map { it.toString() }, file, editor.document)
    }
    
    // null signalizes about cancelling operation
    private fun chooseImports(ambiguousImports: List<List<TypeNameMatch>>): List<TypeNameMatch>? {
        val labelProvider = TypeNameMatchLabelProvider(TypeNameMatchLabelProvider.SHOW_FULLYQUALIFIED)
        
        val dialog = MultiElementListSelectionDialog(getShell(), labelProvider)
        dialog.setTitle(ActionMessages.OrganizeImportsAction_selectiondialog_title)
        dialog.setMessage(ActionMessages.OrganizeImportsAction_selectiondialog_message)
        
        val arrayImports = Array<Array<TypeNameMatch>>(ambiguousImports.size) { i -> 
            Array<TypeNameMatch>(ambiguousImports[i].size) { j ->
                ambiguousImports[i][j]
            }
        }
        
        dialog.setElements(arrayImports)
        
        return if (dialog.open() == Window.OK) {
                 dialog.result.mapNotNull { (it as Array<*>).firstOrNull() as? TypeNameMatch }
            } else {
                null
            }
    }
    
    private fun findTypesToImport(typeNames: List<String>): UniqueAndAmbiguousImports {
        val uniqueImports = arrayListOf<TypeNameMatch>()
        val ambiguousImports = arrayListOf<List<TypeNameMatch>>()
        loop@ for (typeName in typeNames) {
            val typesToImport = findApplicableTypes(typeName)
            when (typesToImport.size) {
                0 -> continue@loop
                1 -> uniqueImports.add(typesToImport.first())
                else -> ambiguousImports.add(typesToImport)
            }
        }
        
        return UniqueAndAmbiguousImports(uniqueImports, ambiguousImports)
    }
    
    private data class UniqueAndAmbiguousImports(
            val uniqueImports: List<TypeNameMatch>, 
            val ambiguousImports: List<List<TypeNameMatch>>)
}

fun prepareOptimizedImports(file: KtFile,
                            descriptorsToImport: Collection<DeclarationDescriptor>): List<ImportPath>? {
    
    // TODO: obtains these constants from the KotlinCodeStyleSettings
    return prepareOptimizedImports(file, descriptorsToImport, 5, 3) { fqName -> false }
}