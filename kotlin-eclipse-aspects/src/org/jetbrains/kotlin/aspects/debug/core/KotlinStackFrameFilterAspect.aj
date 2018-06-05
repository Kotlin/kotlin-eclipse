package org.jetbrains.kotlin.aspects.debug.core;

import java.util.stream.Stream;

import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IVariable;
import org.jetbrains.kotlin.core.utils.DebugUtils;

public aspect KotlinStackFrameFilterAspect {
    pointcut getVariables(IStackFrame receiver) :
		execution(* IStackFrame.getVariables()) &&
		target(receiver);

    @SuppressAjWarnings({ "adviceDidNotMatch" })
    IVariable[] around(IStackFrame receiver) throws DebugException : getVariables(receiver) {
        IVariable[] result = proceed(receiver);

        if (DebugUtils.hasKotlinSource(receiver)) {
            result = Stream.of(result)
                    .filter(DebugUtils::isVisible)
                    .toArray(IVariable[]::new);
        }

        return result;
    }

}
