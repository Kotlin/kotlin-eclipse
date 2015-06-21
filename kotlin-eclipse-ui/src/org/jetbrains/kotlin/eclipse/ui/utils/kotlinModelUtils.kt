package org.jetbrains.kotlin.eclipse.ui.utils

import org.eclipse.jdt.core.IJavaProject
import org.jetbrains.kotlin.core.model.KotlinNature
import org.eclipse.core.resources.IProject
import java.util.ArrayList
import org.eclipse.core.resources.ICommand
import org.jetbrains.kotlin.core.utils.ProjectUtils
import org.jetbrains.kotlin.core.KotlinClasspathContainer
import org.jetbrains.kotlin.core.model.KotlinJavaManager

fun unconfigureKotlinProject(javaProject: IJavaProject) {
	val project = javaProject.getProject()
	
	unconfigureKotlinNature(project)
	unconfigureKotlinRuntime(javaProject)
	removeKotlinBinFolder(project)
}

fun unconfigureKotlinNature(project: IProject) {
	if (KotlinNature.hasKotlinNature(project)) {
		val description = project.getDescription()
		val newNatures = description.getNatureIds().filter { it != KotlinNature.KOTLIN_NATURE }
		
		description.setNatureIds(newNatures.toTypedArray())
		project.setDescription(description, null)
	}
}

fun unconfigureKotlinRuntime(javaProject: IJavaProject) {
	if (ProjectUtils.hasKotlinRuntime(javaProject.getProject())) {
		val newEntries = javaProject.getRawClasspath().filter {
			it != KotlinClasspathContainer.getKotlinRuntimeContainerEntry()
		}
		
		javaProject.setRawClasspath(newEntries.toTypedArray(), null)
	}
}

fun removeKotlinBinFolder(project: IProject) {
	val kotlinBinFolder = KotlinJavaManager.INSTANCE.getKotlinBinFolderFor(project)
	kotlinBinFolder.delete(true, null)
}

fun canBeDeconfigured(project: IProject): Boolean {
	return when {
		KotlinNature.hasKotlinNature(project), ProjectUtils.hasKotlinRuntime(project) -> true
		else -> false
	}
}

fun isConfigurationMissing(project: IProject): Boolean {
	return when {
		!KotlinNature.hasKotlinNature(project), !ProjectUtils.hasKotlinRuntime(project) -> true
		else -> false
	}
}