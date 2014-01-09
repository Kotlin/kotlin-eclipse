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
import org.eclipse.jface.text.templates.TemplateContextType;

public class KotlinTemplateContextType extends TemplateContextType {
    
    public static final String CONTEXT_TYPE_REGISTRY = "org.jetbrains.kotlin.ui.editors.KotlinEditor";
    public static final String KOTLIN_ID_TOP_LEVEL_DECLARATIONS = "kotlin-top-level-declarations";

    public KotlinTemplateContextType() {
        addResolver(new GlobalTemplateVariables.Cursor());
        addResolver(new GlobalTemplateVariables.WordSelection());
        addResolver(new GlobalTemplateVariables.Dollar());
        addResolver(new GlobalTemplateVariables.Date());
        addResolver(new GlobalTemplateVariables.Year());
        addResolver(new GlobalTemplateVariables.Time());
        addResolver(new GlobalTemplateVariables.User());
    }
}
