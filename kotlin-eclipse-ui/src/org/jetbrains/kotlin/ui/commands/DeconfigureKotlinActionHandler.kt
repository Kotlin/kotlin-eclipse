package org.jetbrains.kotlin.ui.commands

import org.eclipse.core.commands.AbstractHandler
import org.eclipse.core.commands.ExecutionEvent
import org.eclipse.ui.handlers.HandlerUtil
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jface.viewers.IStructuredSelection
import org.jetbrains.kotlin.core.model.unconfigureKotlinProject
import org.jetbrains.kotlin.core.model.canBeDeconfigured
import org.eclipse.ui.ISources
import org.jetbrains.kotlin.core.model.KotlinNature
import org.jetbrains.kotlin.core.utils.ProjectUtils
import org.eclipse.jdt.core.JavaCore

public class DeconfigureKotlinActionHandler : AbstractHandler() {
	override fun execute(event: ExecutionEvent): Any? {
		val selection = HandlerUtil.getActiveMenuSelection(event)
		val project = getFirstOrNullProject(selection as IStructuredSelection)
		unconfigureKotlinProject(JavaCore.create(project!!))
		
		return null
	}
	
	override fun setEnabled(evaluationContext: Any) {
		val selection = HandlerUtil.getVariable(evaluationContext, ISources.ACTIVE_CURRENT_SELECTION_NAME)
		if (selection is IStructuredSelection) {
			val project = getFirstOrNullProject(selection)
			if (project != null) {
				setBaseEnabled(canBeDeconfigured(project))
				return
			}
		}
		 
		setBaseEnabled(false)
	}
}