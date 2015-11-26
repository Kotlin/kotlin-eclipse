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
package org.jetbrains.kotlin.ui.editors.quickassist

import java.util.Collections
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import com.intellij.psi.PsiElement

abstract class KotlinQuickAssistProposalsGenerator : KotlinQuickAssist() {
    fun getProposals(): List<KotlinQuickAssistProposal> {
        val activeEditor = getActiveEditor()
        if (activeEditor == null) return emptyList()
        
        val activeElement = getActiveElement()
        if (activeElement == null) return emptyList()
        
        return getProposals(activeEditor, activeElement)
    }
    
    fun hasProposals(): Boolean = getProposals().isNotEmpty()
    
    protected abstract fun getProposals(kotlinFileEditor: KotlinFileEditor, psiElement: PsiElement): List<KotlinQuickAssistProposal>
}