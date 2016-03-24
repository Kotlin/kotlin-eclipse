/*******************************************************************************
 * Copyright 2000-2016 JetBrains s.r.o.
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

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.jface.action.Action;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil;
import org.jetbrains.kotlin.ui.formatter.KotlinFormatterKt;

public class KotlinFormatAction extends Action {
    
    public static final String FORMAT_ACTION_TEXT = "Format";
    
    @NotNull
    private final KotlinFileEditor editor;
    
    public KotlinFormatAction(@NotNull KotlinFileEditor editor) {
        this.editor = editor;
        
        setText(FORMAT_ACTION_TEXT);
        setActionDefinitionId(IJavaEditorActionDefinitionIds.FORMAT);
    }
    
    @Override
    public void run() {
        String sourceCode = EditorUtil.getSourceCode(editor);
        IFile file = EditorUtil.getFile(editor);
        
        if (file == null) {
            KotlinLogger.logError("Failed to retrieve IFile from editor " + editor, null);
            return;
        }
        
        IJavaProject javaProject = editor.getJavaProject();
        if (javaProject == null) {
            KotlinLogger.logError("Failed to format code as java project is null for editor " + editor, null);
            return;
        }
        
        String formattedCode = KotlinFormatterKt.formatCode(sourceCode, javaProject, EditorUtil.getDocumentLineDelimiter(editor));
        editor.getDocument().set(formattedCode);
        
        KotlinPsiManager.getKotlinFileIfExist(file, formattedCode);
    }
}