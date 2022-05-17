package org.jetbrains.kotlin.eclipse.ui.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

object KotlinEclipseScope : CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.IO)
