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
package org.jetbrains.kotlin.core.builder;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.runtime.CoreException;
import org.jetbrains.kotlin.core.log.KotlinLogger;

public class ResourceChangeListener implements IResourceChangeListener {

    @Override
    public void resourceChanged(IResourceChangeEvent event) {
        switch (event.getType()) {
            case IResourceChangeEvent.POST_CHANGE:
                try {
                    event.getDelta().accept(new ProjectChangeListener());
                } catch (CoreException e) {
                    KotlinLogger.logError(e);
                }
                break;
        }
    }

}