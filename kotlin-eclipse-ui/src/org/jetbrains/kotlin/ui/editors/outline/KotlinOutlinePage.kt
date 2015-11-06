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
package org.jetbrains.kotlin.ui.editors.outline

import org.eclipse.jface.viewers.ITreeSelection
import org.eclipse.jface.viewers.SelectionChangedEvent
import org.eclipse.jface.viewers.TreeViewer
import org.eclipse.swt.widgets.Composite
import org.eclipse.ui.views.contentoutline.ContentOutlinePage
import org.jetbrains.kotlin.eclipse.ui.utils.getTextDocumentOffset
import org.jetbrains.kotlin.ui.editors.KotlinEditor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class KotlinOutlinePage(val editor: KotlinEditor) : ContentOutlinePage() {
    override fun createControl(parent: Composite?) {
        super.createControl(parent)
        
        with(getTreeViewer()) {
            setContentProvider(PsiContentProvider())
            setLabelProvider(PsiLabelProvider())
            addSelectionChangedListener(this@KotlinOutlinePage)
        }
        setInputAndExpand()
    }
    
    override fun selectionChanged(event: SelectionChangedEvent) {
        super.selectionChanged(event)
        
        val treeSelection = event.getSelection()
        if (treeSelection is ITreeSelection && !treeSelection.isEmpty()) {
            val psiElement = treeSelection.getFirstElement() as PsiElement
            val offset = psiElement.getTextDocumentOffset(editor.document)
            editor.javaEditor.selectAndReveal(offset, 0) //TODO: reveal Kotlin Element?
        }
    }
    
    private fun setInputAndExpand() {
        with(getTreeViewer()) {
            val psiFile = editor.parsedFile
            setInput(psiFile)
            expandAll()
        }
    }
    
    private fun refresh() {
        getTreeViewer()?.let {
            if (it.getControl().isDisposed()) {
                setInputAndExpand()
            }
        }
    }
}