package org.jetbrains.kotlin.core.log;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jetbrains.kotlin.core.Activator;

public class KotlinLogger {

    public static void log(IStatus status) {
        Activator.getDefault().getLog().log(status);
    }
    
    public static void log(int severity, String message, Throwable exception) {
        log(new Status(severity, Activator.PLUGIN_ID, message, exception));
    }
    
    public static void logError(Throwable exception) {
        log(IStatus.ERROR, "Unexpected Exception", exception);
    }
    
    public static void logError(String message, Throwable exception) {
        log(IStatus.ERROR, message, exception);
    }
    
    public static void logInfo(String message) {
        log(IStatus.INFO, message, null);
    }
    
    public static void logAndThrow(Throwable exception) {
        logError(exception);
        throw new RuntimeException(exception);
    }
}