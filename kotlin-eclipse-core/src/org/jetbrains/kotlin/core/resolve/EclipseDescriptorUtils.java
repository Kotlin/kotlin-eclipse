package org.jetbrains.kotlin.core.resolve;

import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor;
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor.Kind;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptorWithSource;
import org.jetbrains.jet.lang.descriptors.SourceElement;

import com.google.common.collect.Lists;

// Note: copied with some changes from DescriptorToSourceUtils
public class EclipseDescriptorUtils {
    // NOTE this is also used by KDoc
    @Nullable
    public static SourceElement descriptorToDeclaration(@NotNull DeclarationDescriptor descriptor) {
        if (descriptor instanceof CallableMemberDescriptor) {
            return callableDescriptorToDeclaration((CallableMemberDescriptor) descriptor);
        } else if (descriptor instanceof ClassDescriptor) {
            return classDescriptorToDeclaration((ClassDescriptor) descriptor);
        } else {
            return doGetDescriptorToDeclaration(descriptor);
        }
    }

    @NotNull
    public static List<SourceElement> descriptorToDeclarations(@NotNull DeclarationDescriptor descriptor) {
        if (descriptor instanceof CallableMemberDescriptor) {
            return callableDescriptorToDeclarations((CallableMemberDescriptor) descriptor);
        } else {
            SourceElement sourceElement = descriptorToDeclaration(descriptor);
            if (sourceElement != null) {
                return Lists.newArrayList(sourceElement);
            } else {
                return Lists.newArrayList();
            }
        }
    }

    @Nullable
    public static SourceElement callableDescriptorToDeclaration(@NotNull CallableMemberDescriptor callable) {
        if (callable.getKind() == Kind.DECLARATION || callable.getKind() == Kind.SYNTHESIZED) {
            return doGetDescriptorToDeclaration(callable);
        }
        //TODO: should not use this method for fake_override and delegation
        Set<? extends CallableMemberDescriptor> overriddenDescriptors = callable.getOverriddenDescriptors();
        if (overriddenDescriptors.size() == 1) {
            return callableDescriptorToDeclaration(overriddenDescriptors.iterator().next());
        }
        return null;
    }

    @NotNull
    public static List<SourceElement> callableDescriptorToDeclarations(@NotNull CallableMemberDescriptor callable) {
        if (callable.getKind() == Kind.DECLARATION || callable.getKind() == Kind.SYNTHESIZED) {
            SourceElement sourceElement = doGetDescriptorToDeclaration(callable);
            return sourceElement != null ? Lists.newArrayList(sourceElement) : Lists.<SourceElement>newArrayList();
        }

        List<SourceElement> r = Lists.newArrayList();
        Set<? extends CallableMemberDescriptor> overriddenDescriptors = callable.getOverriddenDescriptors();
        for (CallableMemberDescriptor overridden : overriddenDescriptors) {
            r.addAll(callableDescriptorToDeclarations(overridden));
        }
        return r;
    }
    
    @Nullable
    public static SourceElement classDescriptorToDeclaration(@NotNull ClassDescriptor clazz) {
        return doGetDescriptorToDeclaration(clazz);
    }
    
    @Nullable
    private static SourceElement doGetDescriptorToDeclaration(@NotNull DeclarationDescriptor descriptor) {
        DeclarationDescriptor original = descriptor.getOriginal();
        if (!(original instanceof DeclarationDescriptorWithSource)) {
            return null;
        }
        return ((DeclarationDescriptorWithSource) original).getSource();
    }
}
