package org.jetbrains.kotlin.preferences.compiler

import org.eclipse.core.runtime.CoreException
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.Status
import org.eclipse.core.runtime.jobs.Job
import org.jetbrains.kotlin.core.Activator

class RebuildJob(private val rebuildTask: (IProgressMonitor?) -> Unit) : Job("Rebuilding workspace") {

    init {
        priority = BUILD
    }

    override fun run(monitor: IProgressMonitor?): IStatus = try {
        rebuildTask(monitor)
        Status.OK_STATUS
    } catch (e: CoreException) {
        Status(Status.ERROR, Activator.PLUGIN_ID, "Error during build of the project", e)
    }
}