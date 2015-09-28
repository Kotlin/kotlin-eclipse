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
package org.jetbrains.kotlin.ui.refactorings.rename

import org.eclipse.jdt.ui.actions.SelectionDispatchAction
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.eclipse.jface.text.ITextSelection
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds

public class KotlinRenameAction(val kotlinEditor: KotlinFileEditor) : SelectionDispatchAction(kotlinEditor.getSite()) {
    init {
        setActionDefinitionId(IJavaEditorActionDefinitionIds.RENAME_ELEMENT)
    }
    
    companion object {
        val ACTION_ID = "RenameElement"
    }
    
    override fun run(selection: ITextSelection) {
        println("Kotlin Rename Action activated")
    }
}