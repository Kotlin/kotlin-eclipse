/*******************************************************************************
 * Copyright 2000-2015 JetBrains s.r.o.
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

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds
import org.eclipse.jdt.ui.actions.SelectionDispatchAction
import org.eclipse.jdt.ui.refactoring.RefactoringSaveHelper
import org.eclipse.jface.text.ITextSelection
import org.eclipse.ui.PlatformUI
import org.jetbrains.kotlin.ui.editors.KotlinCommonEditor

public class KotlinExtractVariableAction(val editor: KotlinCommonEditor) : SelectionDispatchAction(editor.getSite()) {
    init {
        setActionDefinitionId(IJavaEditorActionDefinitionIds.EXTRACT_LOCAL_VARIABLE)
        setText(RefactoringMessages.ExtractTempAction_label)
        PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.EXTRACT_TEMP_ACTION)
    }
    
    companion object {
        val ACTION_ID = "ExtractLocalVariable"
    }
    
    override fun run(selection: ITextSelection) {
        RefactoringStarter().activate(
                KotlinExtractVariableWizard(KotlinExtractVariableRefactoring(selection, editor)), 
                getShell(), 
                RefactoringMessages.ExtractTempAction_extract_temp, 
                RefactoringSaveHelper.SAVE_NOTHING)
    }
}