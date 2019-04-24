package org.jetbrains.kotlin.core.resolve

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext

object KotlinAnalysisScope : CoroutineScope {

    private val job: Job by lazy { SupervisorJob() }

    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Default

	@JvmStatic
    fun join() = runBlocking {
        job.children.toList().joinAll()
    }
}