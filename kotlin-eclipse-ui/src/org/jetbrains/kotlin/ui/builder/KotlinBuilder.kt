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
package org.jetbrains.kotlin.ui.builder

import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.IncrementalProjectBuilder
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.jdt.core.JavaCore
import org.jetbrains.kotlin.core.model.KotlinEnvironment
import org.jetbrains.kotlin.core.utils.ProjectUtils


class KotlinBuilder : IncrementalProjectBuilder() {

    companion object {
        private val RESOURCE_COPY_EXCLUSION_FILTER_VALUE_PATTERN = Regex("^(?:.*,)?\\*\\.kt(?:,.*)?$")
        private const val RESOURCE_COPY_EXCLUSION_FILTER_NAME =
            "org.eclipse.jdt.core.builder.resourceCopyExclusionFilter"
        private const val RESOURCE_COPY_EXCLUSION_FILTER_VALUE = "*.kt"
    }

    private val oldBuilderElement: KotlinBuilderElement by lazy { KotlinBuilderElement() }

    private val incrementalBuilder: IncrementalKotlinBuilderElement by lazy { IncrementalKotlinBuilderElement() }

    override fun build(kind: Int, args: Map<String, String>?, monitor: IProgressMonitor?): Array<IProject>? {
        val javaProject = JavaCore.create(project)
        val delta = getDelta(project)
        if (KotlinEnvironment.getEnvironment(javaProject.project).buildingProperties.useIncremental) {
            incrementalBuilder
        } else {
            oldBuilderElement
        }.build(project, delta, kind)

        return ProjectUtils.getDependencyProjects(javaProject).toTypedArray()
    }

    override fun startupOnInitialize() {
        super.startupOnInitialize()
        with(JavaCore.create(project)) {
            (getOption(RESOURCE_COPY_EXCLUSION_FILTER_NAME, false) ?: "").let { value ->
                if (!RESOURCE_COPY_EXCLUSION_FILTER_VALUE_PATTERN.matches(value))
                    setOption(
                        RESOURCE_COPY_EXCLUSION_FILTER_NAME,
                        "${if (value.isNotBlank()) "$value," else ""}$RESOURCE_COPY_EXCLUSION_FILTER_VALUE"
                    )
            }
        }
    }
}