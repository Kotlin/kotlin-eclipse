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
package org.jetbrains.kotlin.preferences

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer
import org.jetbrains.kotlin.ui.Activator
import org.jetbrains.kotlin.ui.KotlinPluginUpdater
import java.util.Random

class KotlinPreferenceInitializer : AbstractPreferenceInitializer() {
    override fun initializeDefaultPreferences() {
        val kotlinStore = Activator.getDefault().preferenceStore
        with(kotlinStore) {
            setDefault(KotlinPluginUpdater.LAST_UPDATE_CHECK, 0L)
            setDefault(KotlinPluginUpdater.USER_ID, 0L)
        }
    }
}