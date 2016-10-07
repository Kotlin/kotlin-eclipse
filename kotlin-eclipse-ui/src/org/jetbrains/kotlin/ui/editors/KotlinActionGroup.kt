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

package org.jetbrains.kotlin.ui.editors

import org.eclipse.jdt.internal.ui.actions.JDTQuickMenuCreator
import org.eclipse.jdt.ui.actions.GenerateActionGroup
import org.eclipse.jdt.ui.actions.RefactorActionGroup
import org.eclipse.jface.action.IMenuManager
import org.eclipse.jface.action.MenuManager
import org.eclipse.jface.action.Separator
import org.eclipse.ui.actions.ActionGroup
import org.eclipse.ui.handlers.IHandlerActivation
import org.eclipse.ui.handlers.IHandlerService
import org.eclipse.ui.texteditor.ITextEditorActionConstants
import org.jetbrains.kotlin.ui.editors.organizeImports.KotlinOrganizeImportsAction
import org.jetbrains.kotlin.ui.overrideImplement.KotlinOverrideMembersAction
import org.jetbrains.kotlin.ui.refactorings.extract.KotlinExtractVariableAction
import org.jetbrains.kotlin.ui.refactorings.rename.KotlinRenameAction

class KotlinRefactorActionGroup(editor: KotlinCommonEditor, menuId: String, menuLabel: String, quickMenuId: String) :
        KotlinActionGroup(editor, menuId, menuLabel, quickMenuId) {
    
    init {
        installQuickAccessAction()
    }
    
    override fun fillSubmenu(submenu: IMenuManager) {
        with(submenu) {
            add(Separator(RefactorActionGroup.GROUP_REORG))
            addAction(KotlinRenameAction.ACTION_ID)
            
            add(Separator(RefactorActionGroup.GROUP_CODING))
            addAction(KotlinExtractVariableAction.ACTION_ID)
        }
    }
}

class KotlinGenerateActionGroup(editor: KotlinCommonEditor, menuId: String, menuLabel: String, quickMenuId: String) :
        KotlinActionGroup(editor, menuId, menuLabel, quickMenuId) {
    
    init {
        installQuickAccessAction()
    }
    
    override fun fillSubmenu(submenu: IMenuManager) {
        with(submenu) {
            add(Separator("commentGroup"))
            addAction("ToggleComment")
            addAction("AddBlockComment")
            addAction("RemoveBlockComment")
            
            add(Separator("editGroup"))
            addAction("Indent")
            addAction("Format")
            addAction("QuickFormat")
            
            add(Separator(GenerateActionGroup.GROUP_IMPORT))
            addAction(KotlinOrganizeImportsAction.ACTION_ID)
            
            add(Separator(GenerateActionGroup.GROUP_GENERATE))
            addAction(KotlinOverrideMembersAction.ACTION_ID)
        }
    }
}

abstract class KotlinActionGroup(
        private val editor: KotlinCommonEditor,
        private val menuId: String,
        private val menuLabel: String,
        private val quickMenuId: String) : ActionGroup() {

    var handlerService: IHandlerService? = null
    var menuHandlerActivation: IHandlerActivation? = null
    
    override fun fillContextMenu(menu: IMenuManager) {
        super.fillContextMenu(menu)
        addSubmenu(menu)
    }
    
    abstract fun fillSubmenu(submenu: IMenuManager)
    
    protected fun installQuickAccessAction() {
        handlerService = editor.site.getService(IHandlerService::class.java) ?: return
        
        val menuHandler = object : JDTQuickMenuCreator(editor.javaEditor) {
            override fun fillMenu(menu: IMenuManager) {
                fillSubmenu(menu)
            }
        }.createHandler()
        
        menuHandlerActivation = handlerService?.activateHandler(quickMenuId, menuHandler)
    }
    
    protected fun IMenuManager.addAction(actionId: String) {
        val action = editor.getAction(actionId)
        if (action != null && action.isEnabled) {
            add(action)
        }
    }
    
    override fun dispose() {
        super.dispose()
        
        if (handlerService != null && menuHandlerActivation != null) {
            handlerService!!.deactivateHandler(menuHandlerActivation)
        }
    }
    
    private fun addSubmenu(menu: IMenuManager) {
        val submenu = MenuManager(menuLabel, menuId).apply { setActionDefinitionId(quickMenuId) }
        
        fillSubmenu(submenu)
        
        menu.appendToGroup(ITextEditorActionConstants.GROUP_EDIT, submenu)
    }
}
