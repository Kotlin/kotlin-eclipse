/*******************************************************************************
* Copyright 2000-2015 JetBrains s.r.o.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*
*******************************************************************************/
package org.jetbrains.kotlin.ui.builder

import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.NullProgressMonitor
import org.eclipse.core.runtime.Status
import org.eclipse.core.runtime.jobs.IJobChangeEvent
import org.eclipse.core.runtime.jobs.Job
import org.eclipse.core.runtime.jobs.JobChangeAdapter
import org.eclipse.jdt.core.IJavaProject
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.core.model.KotlinAnalysisProjectCache
import org.jetbrains.kotlin.progress.CompilationCanceledException
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus

public class KotlinAnalysisJob(private val javaProject: IJavaProject) : Job("Kotlin Analysis") {
    init {
        setPriority(DECORATE)
        setSystem(true)
    }
    
    val familyIndicator = constructFamilyIndicator(javaProject)
    
    @Volatile var canceled = false
    
    override fun run(monitor: IProgressMonitor): IStatus {
        try {
            canceled = false
            
            ProgressIndicatorAndCompilationCanceledStatus.setCompilationCanceledStatus(object : CompilationCanceledStatus {
                override fun checkCanceled() {
                    if (canceled) throw CompilationCanceledException()
                }
            })
            
            if (!javaProject.isOpen) {
                return Status.OK_STATUS
            }
            
            val analysisResult = KotlinAnalysisProjectCache.getAnalysisResult(javaProject)
            
            return AnalysisResultStatus(Status.OK_STATUS, analysisResult)
        } catch (e: CompilationCanceledException) {
            return AnalysisResultStatus(Status.CANCEL_STATUS, AnalysisResult.EMPTY)
        } finally {
            ProgressIndicatorAndCompilationCanceledStatus.setCompilationCanceledStatus(null)
        }
    }
    
    override fun belongsTo(family: Any): Boolean = family == familyIndicator
    
    override fun canceling() {
        super.canceling()
        canceled = true
    }
    
    class AnalysisResultStatus(val status: IStatus, val analysisResult: AnalysisResult): IStatus by status
}

private fun constructFamilyIndicator(javaProject: IJavaProject): String {
    return javaProject.getProject().getName() + "_kotlinAnalysisFamily"
}

fun runCancellableAnalysisFor(javaProject: IJavaProject, postAnalysisTask: (AnalysisResult) -> Unit = {}) {
    val family = constructFamilyIndicator(javaProject)
    Job.getJobManager().cancel(family)
    Job.getJobManager().join(family, NullProgressMonitor()) // It should be fast enough
    
    val analysisJob = KotlinAnalysisJob(javaProject)
    
    analysisJob.addJobChangeListener(object : JobChangeAdapter() {
        override fun done(event: IJobChangeEvent) {
            val result = event.result
            if (result is KotlinAnalysisJob.AnalysisResultStatus && result.isOK) {
                postAnalysisTask(result.analysisResult)
            }
        }
    })
    
    analysisJob.schedule()
}