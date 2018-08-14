package org.jetbrains.kotlin.ui

import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.core.resources.WorkspaceJob
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.Status
import org.eclipse.core.runtime.jobs.Job
import org.jetbrains.kotlin.core.filesystem.EnvironmentRemnantNature

class RemoveRemnantProjectsJob: WorkspaceJob("Removing outdated script environments") {

    init {
        isSystem = true
        priority = Job.DECORATE
        rule = ResourcesPlugin.getWorkspace().root
    }

    override fun runInWorkspace(monitor: IProgressMonitor): IStatus {
        ResourcesPlugin.getWorkspace().root.projects
            .asSequence()
            .filter { it.isOpen }
            .filter { it.hasNature(EnvironmentRemnantNature.NATURE_ID) }
            .forEach { it.delete(false, false, null) }

        return Status.OK_STATUS
    }

}
