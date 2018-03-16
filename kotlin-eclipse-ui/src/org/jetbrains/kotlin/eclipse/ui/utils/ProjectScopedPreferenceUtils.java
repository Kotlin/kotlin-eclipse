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
package org.jetbrains.kotlin.eclipse.ui.utils;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.ui.Activator;
import org.osgi.service.prefs.BackingStoreException;

public class ProjectScopedPreferenceUtils {
    
    @NotNull
    private static IEclipsePreferences getPreferences(@NotNull final IProject project) {
        return new ProjectScope(project).getNode(Activator.Companion.getPLUGIN_ID());        
    }
    
    public static boolean getBooleanPreference(@NotNull final IProject project, @NotNull final String key, final boolean defaultValue) {
        return getPreferences(project).getBoolean(key, defaultValue);
    }
    
    public static void putBooleanPreference(@NotNull final IProject project, @NotNull final String key, final boolean value) throws BackingStoreException {
        IEclipsePreferences preferences = getPreferences(project);
        preferences.putBoolean(key, value);
        
        preferences.flush();
    }
}