package org.jetbrains.kotlin.core.resolve;

import org.eclipse.jdt.core.dom.IBinding;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.util.slicedmap.Slices;
import org.jetbrains.jet.util.slicedmap.WritableSlice;

public class EclipseBindingContext {
    public static WritableSlice<IBinding, ConstructorDescriptor> ECLIPSE_CONSTRUCTOR = 
            Slices.<IBinding, ConstructorDescriptor>sliceBuilder().build();
    
    public static WritableSlice<IBinding, VariableDescriptor> ECLIPSE_VARIABLE =
            Slices.<IBinding, VariableDescriptor>sliceBuilder().build();

    public static WritableSlice<IBinding, ClassDescriptor> ECLIPSE_CLASS = 
            Slices.<IBinding, ClassDescriptor>sliceBuilder().build();

    public static WritableSlice<IBinding, SimpleFunctionDescriptor> ECLIPSE_FUNCTION = 
            Slices.<IBinding, SimpleFunctionDescriptor>sliceBuilder().build();
}
