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
package org.jetbrains.kotlin.ui.refactorings.extract

import org.eclipse.ltk.ui.refactoring.RefactoringWizard
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages
import org.eclipse.jdt.internal.ui.refactoring.TextInputWizardPage
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.GridLayout
import org.eclipse.jdt.internal.ui.util.RowLayouter
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.layout.GridData
import org.eclipse.jface.dialogs.Dialog
import org.eclipse.ui.PlatformUI
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent

class KotlinExtractVariableWizard(val refactoring: KotlinExtractVariableRefactoring) : 
        RefactoringWizard(refactoring, RefactoringWizard.DIALOG_BASED_USER_INTERFACE) {
    init {
        setDefaultPageTitle(RefactoringMessages.ExtractTempWizard_defaultPageTitle)
    }
    
    override fun addUserInputPages() {
        addPage(ExtractVariableInputPage())
    }
    
    private class ExtractVariableInputPage : 
            TextInputWizardPage(RefactoringMessages.ExtractTempInputPage_enter_name, true, "") {
        val kotlinRefactoring by lazy { getRefactoring() as KotlinExtractVariableRefactoring }
        
        override fun textModified(text: String) {
            kotlinRefactoring.newName = text
            super.textModified(text)
        }
    
        override fun createControl(parent: Composite) {
            val result = Composite(parent, SWT.NONE)
            setControl(result)
            
            val layout = GridLayout()
            layout.numColumns = 2
            layout.verticalSpacing = 8
            result.setLayout(layout)
            val layouter = RowLayouter(2)
            
            val label= Label(result, SWT.NONE)
            label.setText(RefactoringMessages.ExtractTempInputPage_variable_name)
            
            val text = createTextInputField(result)
            text.selectAll()
            text.setLayoutData(GridData(GridData.FILL_HORIZONTAL))
            
            layouter.perform(label, text, 1)
            
            addReplaceAllCheckbox(result, layouter)
            
            Dialog.applyDialogFont(result)
            PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJavaHelpContextIds.EXTRACT_TEMP_WIZARD_PAGE)
        }
        
        private fun addReplaceAllCheckbox(result: Composite, layouter: RowLayouter) {
            val title = RefactoringMessages.ExtractConstantInputPage_replace_all
            val defaultValue = true
            
            val checkBox= Button(result, SWT.CHECK)
            checkBox.setText(title)
            checkBox.setSelection(defaultValue)
            layouter.perform(checkBox)
            
            checkBox.addSelectionListener(object : SelectionAdapter() {
                override fun widgetSelected(e: SelectionEvent) {
                    kotlinRefactoring.replaceAllOccurrences = checkBox.getSelection()
                }
            })
        }
    }
}