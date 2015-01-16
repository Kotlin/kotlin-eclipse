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
package org.jetbrains.kotlin.ui.editors.outline;

import java.util.List;

import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.jetbrains.kotlin.psi.JetClass;
import org.jetbrains.kotlin.psi.JetClassInitializer;
import org.jetbrains.kotlin.psi.JetElement;
import org.jetbrains.kotlin.psi.JetFunction;
import org.jetbrains.kotlin.psi.JetPackageDirective;
import org.jetbrains.kotlin.psi.JetParameter;
import org.jetbrains.kotlin.psi.JetProperty;
import org.jetbrains.kotlin.psi.JetTypeReference;

public class PsiLabelProvider extends LabelProvider {
    
    public static final String CLASS_INITIALIZER = "<class initializer>";
    
    @Override
    public String getText(Object element) {
        if (element instanceof JetElement) {
            return getPresentableElement((JetElement) element);
        }
        
        return "";
    }
    
    @Override
    public Image getImage(Object element) {
        String imageName = null;
        if (element instanceof JetClass) {
            if (((JetClass) element).isTrait()) {
                imageName = ISharedImages.IMG_OBJS_INTERFACE;
            } else {
                imageName = ISharedImages.IMG_OBJS_CLASS;
            }
        } else if (element instanceof JetPackageDirective) {
            imageName = ISharedImages.IMG_OBJS_PACKAGE;
        } else if (element instanceof JetFunction) {
            imageName = ISharedImages.IMG_OBJS_PUBLIC;
        } else if (element instanceof JetProperty) {
            imageName = ISharedImages.IMG_FIELD_PUBLIC;
        }
        
        if (imageName != null) {
            return JavaUI.getSharedImages().getImage(imageName);
        }
        
        return null;
    }
    
    // Source code is taken from org.jetbrains.kotlin.idea.projectView.JetDeclarationTreeNode, updateImple()
    private String getPresentableElement(JetElement declaration) {
        String text = "";
        if (declaration != null) {
            text = declaration.getName();
            if (text == null) return "";
            if (declaration instanceof JetClassInitializer) {
                text = CLASS_INITIALIZER;
            } else if (declaration instanceof JetProperty) {
                JetProperty property = (JetProperty) declaration;
                JetTypeReference ref = property.getTypeReference();
                if (ref != null) {
                    text += " ";
                    text += ":";
                    text += " ";
                    text += ref.getText();
                }
            } else if (declaration instanceof JetFunction) {
                JetFunction function = (JetFunction) declaration;
                JetTypeReference receiverTypeRef = function.getReceiverTypeReference();
                if (receiverTypeRef != null) {
                    text = receiverTypeRef.getText() + "." + text;
                }
                text += "(";
                List<JetParameter> parameters = function.getValueParameters();
                for (JetParameter parameter : parameters) {
                    if (parameter.getName() != null) {
                        text += parameter.getName();
                        text += " ";
                        text += ":";
                        text += " ";
                    }
                    JetTypeReference typeReference = parameter.getTypeReference();
                    if (typeReference != null) {
                        text += typeReference.getText();
                    }
                    text += ", ";
                }
                if (parameters.size() > 0) text = text.substring(0, text.length() - 2);
                text += ")";
                JetTypeReference typeReference = function.getTypeReference();
                if (typeReference != null) {
                    text += " ";
                    text += ":";
                    text += " ";
                    text += typeReference.getText();
                }
            }
        }
        
        return text;
    }
}
