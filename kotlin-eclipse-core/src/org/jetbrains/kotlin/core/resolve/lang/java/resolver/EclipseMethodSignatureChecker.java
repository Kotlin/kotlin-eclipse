package org.jetbrains.kotlin.core.resolve.lang.java.resolver;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.jet.lang.resolve.java.resolver.MethodSignatureChecker;
import org.jetbrains.jet.lang.resolve.java.structure.JavaMethod;

public class EclipseMethodSignatureChecker implements MethodSignatureChecker  {

    @Override
    public void checkSignature(@NotNull JavaMethod method,
            boolean reportSignatureErrors,
            @NotNull SimpleFunctionDescriptor descriptor,
            @NotNull List<String> signatureErrors,
            @NotNull List<FunctionDescriptor> superFunctions) {
        // TODO Auto-generated method stub
    }
}
