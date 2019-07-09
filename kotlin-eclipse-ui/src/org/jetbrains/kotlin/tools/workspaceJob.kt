package org.jetbrains.kotlin.tools

import org.eclipse.core.resources.IResource
import org.eclipse.core.resources.WorkspaceJob
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.Status
import org.eclipse.core.runtime.jobs.Job

fun workspaceJob(
    name: String = "",
    priority: Int = Job.BUILD,
    resource: IResource? = null,
    isSystem: Boolean = false,
    action: (IProgressMonitor?) -> Unit
) {
    object: WorkspaceJob(name) {
        init {
            this.isSystem = isSystem
            this.rule = resource
            this.priority = priority
        }

        override fun runInWorkspace(monitor: IProgressMonitor?): IStatus {
            action(monitor)
            return Status.OK_STATUS
        }
    }.schedule()
}