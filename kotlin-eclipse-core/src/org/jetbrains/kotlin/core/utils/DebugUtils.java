package org.jetbrains.kotlin.core.utils;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.jetbrains.kotlin.core.log.KotlinLogger;

public class DebugUtils {
    public static Boolean isVisible(IVariable variable) {
        try {
            return !variable.getName().startsWith("$i$");
        } catch (Exception e) {
            KotlinLogger.logError(e);
            return false;
        }
    }

    public static Boolean hasKotlinSource(IStackFrame frame) throws DebugException {
        if (frame instanceof IJavaStackFrame) {
            IJavaStackFrame javaFrame = (IJavaStackFrame) frame;
            return javaFrame.getSourceName().endsWith(".kt");
        } else {
            return false;
        }
    }
}
