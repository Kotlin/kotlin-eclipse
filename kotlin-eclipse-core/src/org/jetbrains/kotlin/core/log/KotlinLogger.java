/*******************************************************************************
 * Copyright 2000-2014 JetBrains s.r.o.
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
    
    public static void logWarning(String message) {
        log(IStatus.WARNING, message, null);
    }
    
    public static void logAndThrow(Throwable exception) {
        logError(exception);
        throw new RuntimeException(exception);
    }
}