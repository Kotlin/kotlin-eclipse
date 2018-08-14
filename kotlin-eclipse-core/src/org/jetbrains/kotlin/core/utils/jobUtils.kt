package org.jetbrains.kotlin.core.utils

import org.eclipse.core.resources.IResource
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.jobs.Job

fun <R: IResource, T> withResourceLock(resource: R, monitor: IProgressMonitor? = null, block: (R) -> T): T =
    try {
        Job.getJobManager().beginRule(resource, monitor)
        block(resource)
    } finally {
        Job.getJobManager().endRule(resource)
    }