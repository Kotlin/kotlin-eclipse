package org.jetbrains.kotlin.ui.editors.completion;

import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.swt.graphics.Image;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;

public class KotlinDescriptorUtils {
    
    public static final KotlinDescriptorUtils INSTANCE = new KotlinDescriptorUtils();
    
    private KotlinDescriptorUtils() {
    }
    
    @Nullable
    public Image getImage(@NotNull DeclarationDescriptor descriptor) {
        if (descriptor instanceof ClassDescriptor) {
            return getImageFromJavaUI(ISharedImages.IMG_OBJS_CLASS);
        } else if (descriptor instanceof FunctionDescriptor) {
            return getImageFromJavaUI(ISharedImages.IMG_OBJS_PUBLIC);
        } else if (descriptor instanceof VariableDescriptor) {
            return getImageFromJavaUI(ISharedImages.IMG_FIELD_PUBLIC);
        }
        
        return null;
    }
    
    @Nullable
    private Image getImageFromJavaUI(@NotNull String imageName) {
        return JavaUI.getSharedImages().getImage(imageName);
    }
}
