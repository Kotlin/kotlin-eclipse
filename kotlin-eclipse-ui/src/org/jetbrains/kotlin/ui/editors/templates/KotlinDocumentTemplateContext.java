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
package org.jetbrains.kotlin.ui.editors.templates;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.templates.DocumentTemplateContext;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateBuffer;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.TemplateException;
import org.jetbrains.kotlin.ui.editors.KotlinEditor;

public class KotlinDocumentTemplateContext extends DocumentTemplateContext {

    private final KotlinEditor editor; 
    
    public KotlinDocumentTemplateContext(TemplateContextType type, KotlinEditor editor, int offset, int length) {
        super(type, editor.getDocument(), offset, length);
        this.editor = editor;
    }
    
    @Override
    public TemplateBuffer evaluate(Template template) throws BadLocationException, TemplateException {
        TemplateBuffer templateBuffer = super.evaluate(template);
        
        KotlinTemplateFormatter templateFormatter = new KotlinTemplateFormatter();
        
        IFile file = editor.getEclipseFile();

        assert file != null : "Failed to retrieve IFile from editor " + editor;

        String lineDelimiter = TextUtilities.getDefaultLineDelimiter(getDocument());
        templateFormatter.format(templateBuffer, lineDelimiter, file.getProject());
        
        return templateBuffer;
    }
}
