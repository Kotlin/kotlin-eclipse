package org.jetbrains.kotlin.aspects.navigation;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.core.SourceMapper;
import org.jetbrains.kotlin.core.resolve.KotlinSourceIndex;

@SuppressWarnings("restriction")
public aspect KotlinFindSourceAspect {
    pointcut findSource(SourceMapper mapper, IType type, String simpleSourceFileName) :
        target(mapper) &&
        args(type, simpleSourceFileName)
        && call(char[] findSource(IType, String));
    
    char[] around(SourceMapper mapper, IType type, String simpleSourceFileName) : findSource(mapper, type, simpleSourceFileName) {
        char[] result = proceed(mapper, type, simpleSourceFileName);
        if (result == null && KotlinSourceIndex.isKotlinSource(simpleSourceFileName)) {
            return KotlinSourceIndex.getSource(mapper, type, simpleSourceFileName);
        }
        return result;
    }
}
