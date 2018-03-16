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

import java.io.IOException;

import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.persistence.TemplateStore;
import org.eclipse.ui.editors.text.templates.ContributionContextTypeRegistry;
import org.eclipse.ui.editors.text.templates.ContributionTemplateStore;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.ui.Activator;

public class KotlinTemplateManager {
    
    public static final KotlinTemplateManager INSTANCE = new KotlinTemplateManager();
    
    private TemplateStore templateStore;
    private ContributionContextTypeRegistry contextTypeRegistry;
    
    private final String TEMPLATES_KEY = "org.jetbrains.kotlin.ui.templates.key";
    
    private KotlinTemplateManager() {
    }
    
    public ContextTypeRegistry getContextTypeRegistry() {
        if (contextTypeRegistry == null) {
            contextTypeRegistry = new ContributionContextTypeRegistry(KotlinTemplateContextType.CONTEXT_TYPE_REGISTRY);
        }
        
        contextTypeRegistry.addContextType(KotlinTemplateContextType.KOTLIN_ID_TOP_LEVEL_DECLARATIONS);
        
        return contextTypeRegistry;
    }
    
    public TemplateStore getTemplateStore() {
        if (templateStore == null) {
            templateStore = new ContributionTemplateStore(getContextTypeRegistry(), Activator.Companion.getDefault().getPreferenceStore(), TEMPLATES_KEY);
            try {
                templateStore.load();
            } catch (IOException e) {
                KotlinLogger.logAndThrow(e);
            }
        }
        
        return templateStore;
    }
}
