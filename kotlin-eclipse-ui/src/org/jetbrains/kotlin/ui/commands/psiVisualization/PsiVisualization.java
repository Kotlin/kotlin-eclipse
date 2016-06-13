/*******************************************************************************
 * Copyright 2000-2015 JetBrains s.r.o.
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
package org.jetbrains.kotlin.ui.commands.psiVisualization;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISources;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil;
import org.jetbrains.kotlin.ui.editors.KotlinEditor;
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor;

public class PsiVisualization extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IEditorPart editor = HandlerUtil.getActiveEditor(event);

        assert editor instanceof KotlinFileEditor : "Unsupported editor class: " + editor == null ? "NULL" : editor.getClass().getName();
        
        IFile file = EditorUtil.getFile(editor);
        if (file != null) {
            String sourceCode = EditorUtil.getSourceCode((KotlinEditor) editor);

            new VisualizationPage(HandlerUtil.getActiveShell(event), sourceCode, file).open();
        } else {
            KotlinLogger.logError("Failed to retrieve IFile from editor " + editor, null);
        }
        
        return null;
    }

    @Override
    public void setEnabled(Object evaluationContext) {
        Object editorObject = HandlerUtil.getVariable(evaluationContext, ISources.ACTIVE_EDITOR_NAME);
        setBaseEnabled(editorObject instanceof KotlinFileEditor);
    }

}
