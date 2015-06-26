package org.jetbrains.kotlin.ui.editors.quickassist

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.psi.JetClassOrObject
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import java.util.LinkedHashSet
import org.jetbrains.kotlin.resolve.OverrideResolver
import com.google.common.collect.LinkedHashMultimap

public class KotlinOverrideMethodsProposal : KotlinOverrideImplementMethodsProposal() {
    override public fun collectMethodsToGenerate(classOrObject: JetClassOrObject): Set<CallableMemberDescriptor> {
        
    }
    
    private fun collectSuperMethods(classDescriptor: ClassDescriptor) {
        val inheritedFunctions = LinkedHashSet<CallableMemberDescriptor>()
        for (supertype in classDescriptor.getTypeConstructor().getSupertypes()) {
            supertype.getMemberScope().getAllDescriptors().filterIsInstanceTo(inheritedFunctions)
        }
        
        val filteredMembers = OverrideResolver.filterOutOverridden(inheritedFunctions)
        
        val factoredMembers = LinkedHashMultiMap<CallableMemberDescriptor, CallableMemberDescriptor>()
        for (one in filteredMembers) {
            if (factoredMembers.values().contains(one)) continue
            for (another in filteredMembers) {
                if (one === another) continue
                factoredMembers.put(one, one)
                
            }
        }
    }
    
//    @NotNull
//    private static Set<CallableMemberDescriptor> collectSuperMethods(@NotNull ClassDescriptor classDescriptor) {
//        Set<CallableMemberDescriptor> inheritedFunctions = new LinkedHashSet<CallableMemberDescriptor>();
//        for (JetType supertype : classDescriptor.getTypeConstructor().getSupertypes()) {
//            for (DeclarationDescriptor descriptor : supertype.getMemberScope().getAllDescriptors()) {
//                if (descriptor instanceof CallableMemberDescriptor) {
//                    inheritedFunctions.add((CallableMemberDescriptor) descriptor);
//                }
//            }
//        }
//
//        // Only those actually inherited
//        Set<CallableMemberDescriptor> filteredMembers = OverrideResolver.filterOutOverridden(inheritedFunctions);
//
//        // Group members with "the same" signature
//        Multimap<CallableMemberDescriptor, CallableMemberDescriptor> factoredMembers = LinkedHashMultimap.create();
//        for (CallableMemberDescriptor one : filteredMembers) {
//            if (factoredMembers.values().contains(one)) continue;
//            for (CallableMemberDescriptor another : filteredMembers) {
////                if (one == another) continue;
//                factoredMembers.put(one, one);
//                if (OverridingUtil.DEFAULT.isOverridableBy(one, another).getResult() == OVERRIDABLE
//                    || OverridingUtil.DEFAULT.isOverridableBy(another, one).getResult() == OVERRIDABLE) {
//                    factoredMembers.put(one, another);
//                }
//            }
//        }
//
//        return factoredMembers.keySet();
//    }
    
    override fun getDisplayString(): String = "Override methods"
}