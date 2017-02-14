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

public class DeconfigureKotlinActionHandler : AbstractHandler() {
	override fun execute(event: ExecutionEvent): Any? {
		val selection = HandlerUtil.getActiveMenuSelection(event)
		val project = getFirstOrNullJavaProject(selection as IStructuredSelection)
		unconfigureKotlinProject(project!!)
		
		return null
	}
	
	override fun setEnabled(evaluationContext: Any) {
		val selection = HandlerUtil.getVariable(evaluationContext, ISources.ACTIVE_CURRENT_SELECTION_NAME)
		if (selection is IStructuredSelection) {
			val javaProject = getFirstOrNullJavaProject(selection)
			if (javaProject != null) {
				setBaseEnabled(canBeDeconfigured(javaProject.getProject()))
				return
			}
		}
		 
		setBaseEnabled(false)
	}
}