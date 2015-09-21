package org.jetbrains.kotlin.ui.overrideImplement

import org.eclipse.jdt.ui.actions.SelectionDispatchAction
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.eclipse.jface.text.ITextSelection
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds
import org.eclipse.jdt.internal.ui.actions.ActionMessages
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds
import org.eclipse.ui.PlatformUI

public class KotlinOverrideMembersAction(val editor: KotlinFileEditor) : SelectionDispatchAction(editor.getSite()) {
    init {
        setActionDefinitionId(IJavaEditorActionDefinitionIds.OVERRIDE_METHODS)
        setText(ActionMessages.OverrideMethodsAction_label)
        setDescription(ActionMessages.OverrideMethodsAction_description)
        setToolTipText(ActionMessages.OverrideMethodsAction_tooltip)
        PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.ADD_UNIMPLEMENTED_METHODS_ACTION)
    }
    
    companion object {
        val ACTION_ID = "OverrideMethods"
    }
    
    override fun run(selection: ITextSelection) {
        
    }
}