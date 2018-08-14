package org.jetbrains.kotlin.ui

import org.eclipse.jdt.internal.core.JavaProject
import org.eclipse.jface.viewers.Viewer
import org.eclipse.jface.viewers.ViewerFilter
import org.jetbrains.kotlin.core.filesystem.EnvironmentRemnantNature
import org.jetbrains.kotlin.core.script.EnvironmentProjectsManager

class ScriptEnvironmentsFilter : ViewerFilter() {
    override fun select(viewer: Viewer, parentElement: Any, element: Any): Boolean {
        val project = (element as? JavaProject)?.project ?: return true
        if (!project.isOpen) return true
        return !project.hasNature(EnvironmentRemnantNature.NATURE_ID)
                && !EnvironmentProjectsManager.wasCreated(project.name)
    }
}