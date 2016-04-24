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
        println("Run Kotlin, run!")
    }
}