package org.jetbrains.kotlin.ui.editors.templates;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.templates.DocumentTemplateContext;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateBuffer;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.TemplateException;
import org.jetbrains.kotlin.utils.EditorUtil;

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
        
        IJavaProject javaProject = JavaCore.create(EditorUtil.getFile(editor).getProject());
        templateFormatter.format(templateBuffer, getLineIndentation(), javaProject);
        
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
