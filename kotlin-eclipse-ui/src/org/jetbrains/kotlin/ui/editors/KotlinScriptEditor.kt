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
package org.jetbrains.kotlin.ui.editors

import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jface.text.IDocument
import org.eclipse.ui.PlatformUI
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.core.model.KotlinScriptEnvironment
import org.jetbrains.kotlin.core.model.getEnvironment
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.script.ScriptDependenciesProvider
import org.jetbrains.kotlin.ui.tryUpdateScriptClasspath
import kotlin.script.experimental.dependencies.ScriptDependencies

class KotlinScriptEditor : KotlinCommonEditor() {
    override val parsedFile: KtFile?
        get() {
            val file = eclipseFile ?: return null
            return KotlinPsiManager.getKotlinFileIfExist(file, document.get())
        }

    override val javaProject: IJavaProject? by lazy {
        eclipseFile?.let { JavaCore.create(it.project) }
    }

    override val document: IDocument
        get() = documentProvider.getDocument(editorInput)

    override val isScript: Boolean
        get() = true

    override fun dispose() {
        eclipseFile?.let {
            KotlinScriptEnvironment.removeKotlinEnvironment(it)
            KotlinPsiManager.removeFile(it)
        }
    }

    internal fun reconcile(runBeforeReconciliation: () -> Unit = {}) {
        kotlinReconcilingStrategy.reconcile(runBeforeReconciliation)
    }

    override fun doAfterSemanticHighlightingInstallation() {
        eclipseFile?.let { tryUpdateScriptClasspath(it) }
    }
}

// TODO it is probably broken right now
fun getScriptDependencies(editor: KotlinScriptEditor): ScriptDependencies? {
    val eclipseFile = editor.eclipseFile ?: return null

    val project = getEnvironment(eclipseFile).project
    val definition = ScriptDependenciesProvider.getInstance(project)

    val ktFile = editor.parsedFile ?: return null
    return definition?.getScriptDependencies(ktFile)
}

fun KotlinCommonEditor.isOpen(): Boolean {
    for (window in PlatformUI.getWorkbench().workbenchWindows) {
        for (page in window.pages) {
            for (editorReference in page.editorReferences) {
                if (editorReference.getEditor(false) == this) {
                    return true
                }
            }
        }
    }

    return false
}