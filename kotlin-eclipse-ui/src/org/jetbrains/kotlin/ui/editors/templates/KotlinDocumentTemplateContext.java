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
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.templates.DocumentTemplateContext;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateBuffer;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.TemplateException;
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil;

public class KotlinDocumentTemplateContext extends DocumentTemplateContext {

    private final JavaEditor editor; 
    
    public KotlinDocumentTemplateContext(TemplateContextType type, JavaEditor editor, int offset, int length) {
        super(type, editor.getViewer().getDocument(), offset, length);
        this.editor = editor;
    }
    
    @Override
    public TemplateBuffer evaluate(Template template) throws BadLocationException, TemplateException {
        TemplateBuffer templateBuffer = super.evaluate(template);
        
        KotlinTemplateFormatter templateFormatter = new KotlinTemplateFormatter();
        
        IFile file = EditorUtil.getFile(editor);

        assert file != null : "Failed to retrieve IFile from editor " + editor;

        IJavaProject javaProject = JavaCore.create(file.getProject());
        String lineDelimiter = TextUtilities.getDefaultLineDelimiter(getDocument());
        templateFormatter.format(templateBuffer, getLineIndentation(), lineDelimiter, javaProject);
        
        return templateBuffer;
    }
    
    private int getLineIndentation() {
        int start = getStart();
    
        IDocument document = getDocument();
        try {
            IRegion region = document.getLineInformationOfOffset(start);
            return document.get(region.getOffset(), getStart() - region.getOffset()).length();
        } catch (BadLocationException e) {
            return 0;
        }
    }
}
