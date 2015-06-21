package org.jetbrains.kotlin.ui.commands

import org.eclipse.core.commands.AbstractHandler
import org.eclipse.core.commands.ExecutionEvent
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jface.viewers.IStructuredSelection
import org.eclipse.ui.ISources
import org.jetbrains.kotlin.eclipse.ui.utils.isConfigurationMissing
import org.eclipse.ui.handlers.HandlerUtil
import org.jetbrains.kotlin.wizards.NewUnitWizard
import org.jetbrains.kotlin.ui.launch.KotlinRuntimeConfigurationSuggestor
import org.jetbrains.kotlin.core.model.KotlinNature

public class ConfigureKotlinActionHandler : AbstractHandler() {
	override fun execute(event: ExecutionEvent): Any? {
		val selection = HandlerUtil.getActiveMenuSelection(event)
		val project = getFirstOrNullJavaProject(selection as IStructuredSelection)!!.getProject()
		
		KotlinNature.addNature(project)
        KotlinRuntimeConfigurationSuggestor.suggestForProject(project);
		
		return null
	}
	
	override fun setEnabled(evaluationContext: Any) {
		val selection = HandlerUtil.getVariable(evaluationContext, ISources.ACTIVE_CURRENT_SELECTION_NAME)
		if (selection is IStructuredSelection) {
			val javaProject = getFirstOrNullJavaProject(selection)
			if (javaProject != null) {
				setBaseEnabled(isConfigurationMissing(javaProject.getProject()))
				return
			}
		}
		 
		setBaseEnabled(false)
	}
}