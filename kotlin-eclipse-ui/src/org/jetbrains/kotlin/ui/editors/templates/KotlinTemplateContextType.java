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

import org.eclipse.jface.text.templates.GlobalTemplateVariables;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.TemplateVariableResolver;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.ui.editors.KotlinEditor;

public class KotlinTemplateContextType extends TemplateContextType {
    
    public static final String CONTEXT_TYPE_REGISTRY = "org.jetbrains.kotlin.ui.editors.KotlinFileEditor";
    public static final String KOTLIN_ID_TOP_LEVEL_DECLARATIONS = "kotlin-top-level-declarations";

    public KotlinTemplateContextType() {
        addResolver(new GlobalTemplateVariables.Cursor());
        addResolver(new GlobalTemplateVariables.WordSelection());
        addResolver(new GlobalTemplateVariables.Dollar());
        addResolver(new GlobalTemplateVariables.Date());
        addResolver(new GlobalTemplateVariables.Year());
        addResolver(new GlobalTemplateVariables.Time());
        addResolver(new GlobalTemplateVariables.User());
        addResolver(new File());
    }

    protected static class File extends TemplateVariableResolver {
        public File() {
            super("file", "Name of the source file");  //$NON-NLS-1$
        }
        @Override
        protected String resolve(TemplateContext context) {
            KotlinEditor editor = ((KotlinDocumentTemplateContext) context).getKotlinEditor();

            KtFile parsedFile = editor.getParsedFile();
            if(parsedFile != null) {
                String fileName = parsedFile.getName();
                int fileExtensionIndex = fileName.lastIndexOf(".");
                return fileName.substring(0, fileExtensionIndex);
            }
             
            return null;
        }
        @Override
        protected boolean isUnambiguous(TemplateContext context) {
            return resolve(context) != null;
        }
    }
}
