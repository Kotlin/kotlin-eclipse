/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.core.filesystem

import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.IProjectNature

class EnvironmentRemnantNature: IProjectNature {
    companion object {
        const val NATURE_ID = "org.jetbrains.kotlin.core.environmentRemnant"
    }

    private lateinit var _project: IProject

    override fun setProject(project: IProject) {
        _project = project
    }

    override fun configure() {
        // nothing to do
    }

    override fun deconfigure() {
        // nothing to do
    }

    override fun getProject() = _project
}