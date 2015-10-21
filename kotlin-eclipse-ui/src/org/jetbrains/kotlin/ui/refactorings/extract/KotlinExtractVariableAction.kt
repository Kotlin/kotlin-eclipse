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

import org.eclipse.jdt.ui.actions.SelectionDispatchAction
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds
import org.eclipse.ui.PlatformUI
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds
import org.eclipse.jface.text.ITextSelection
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter
import org.eclipse.jdt.ui.refactoring.RefactoringSaveHelper
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil
import org.jetbrains.kotlin.ui.editors.selection.KotlinSelectEnclosingAction
import org.jetbrains.kotlin.ui.editors.selection.KotlinSemanticSelectionAction
import org.jetbrains.kotlin.core.references.getReferenceExpression
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import com.intellij.psi.util.PsiTreeUtil

public class KotlinExtractVariableAction(val editor: KotlinFileEditor) : SelectionDispatchAction(editor.getSite()) {
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