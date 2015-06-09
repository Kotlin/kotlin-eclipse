package org.jetbrains.kotlin.ui.commands

import org.eclipse.core.commands.AbstractHandler
import org.eclipse.core.commands.ExecutionEvent
import org.eclipse.ui.handlers.HandlerUtil
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jface.viewers.IStructuredSelection
import org.jetbrains.kotlin.eclipse.ui.utils.unconfigureKotlinProject
import org.jetbrains.kotlin.eclipse.ui.utils.canBeDeconfigured
import org.eclipse.ui.ISources
import org.jetbrains.kotlin.core.model.KotlinNature
import org.jetbrains.kotlin.core.utils.ProjectUtils

public class DeconfigureKotlinActionHandler : AbstractHandler() {
	override fun execute(event: ExecutionEvent): Any? {
		val selection = HandlerUtil.getActiveMenuSelection(event)
		val project = getSelectedProject(selection as IStructuredSelection)
		unconfigureKotlinProject(project)
		
		return null
	}
	
	override fun setEnabled(evaluationContext: Any) {
		val selection = HandlerUtil.getVariable(evaluationContext, ISources.ACTIVE_CURRENT_SELECTION_NAME)
		if (selection is IStructuredSelection) {
			val elements = selection.toArray()
			if (elements.size() != 1) {
				setBaseEnabled(false)
				return
			}
			
			val element = elements[0]
			if (element is IJavaProject) {
				val eclipseProject = element.getProject()
				setBaseEnabled(canBeDeconfigured(eclipseProject))
				return
			}
		}
		 
		setBaseEnabled(false)
	}
	
	private fun getSelectedProject(selection: IStructuredSelection): IJavaProject {
		return selection.toArray()[0] as IJavaProject
	}
}