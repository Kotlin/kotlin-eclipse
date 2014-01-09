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
