package org.jetbrains.kotlin.core.log

import org.jetbrains.kotlin.core.Activator
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.Status
import kotlin.jvm.JvmStatic

object KotlinLogger {
	@JvmStatic fun log(status: IStatus) {
		Activator.getDefault().log.log(status)
	}
	
	@JvmStatic fun log(severity: Int, message: String, exception: Throwable?) {
		log(Status(severity, Activator.PLUGIN_ID, message, exception))
	}
	
	@JvmStatic fun logError(exception: Throwable) {
		log(IStatus.ERROR, "Unexpected Exception", exception)
	}
	
	@JvmStatic fun logError(message: String, exception: Throwable?) {
		log(IStatus.ERROR, message, exception)
	}
	
	@JvmStatic fun logInfo(message: String) {
		log(IStatus.INFO, message, null)
	}
	
	@JvmStatic fun logWarning(message: String) {
		log(IStatus.WARNING, message, null)
	}
	
	@JvmStatic fun logAndThrow(exception: Throwable): Nothing {
		logError(exception)
		throw exception
	}
}