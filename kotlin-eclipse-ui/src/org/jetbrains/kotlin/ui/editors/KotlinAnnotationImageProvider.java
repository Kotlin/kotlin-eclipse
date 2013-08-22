package org.jetbrains.kotlin.ui.editors;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.texteditor.IAnnotationImageProvider;

public class KotlinAnnotationImageProvider implements IAnnotationImageProvider {

    @Override
    public Image getManagedImage(Annotation annotation) {
        if (annotation instanceof DiagnosticAnnotation) {
            DiagnosticAnnotation diagnosticAnnotation = (DiagnosticAnnotation) annotation;
            if (diagnosticAnnotation.quickFixable()) {
                switch (diagnosticAnnotation.getType()) {
                    case AnnotationManager.ANNOTATION_ERROR_TYPE:
                        return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_FIXABLE_ERROR);
                    case AnnotationManager.ANNOTATION_WARNING_TYPE:
                        return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_FIXABLE_PROBLEM);
                }
            }
        }
        
        return null;
    }

    @Override
    public String getImageDescriptorId(Annotation annotation) {
        return null;
    }

    @Override
    public ImageDescriptor getImageDescriptor(String imageDescritporId) {
        return null;
    }

}
