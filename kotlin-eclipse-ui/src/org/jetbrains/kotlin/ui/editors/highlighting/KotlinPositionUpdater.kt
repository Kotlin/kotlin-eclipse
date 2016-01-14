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
package org.jetbrains.kotlin.ui.editors.highlighting

import org.eclipse.jface.text.IPositionUpdater
import org.eclipse.jface.text.DocumentEvent
import org.eclipse.jface.text.Position

// Inspired by org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlightingPresenter.HighlightinPositionUpdater
public class KotlinPositionUpdater(val category: String) : IPositionUpdater {
    override fun update(event: DocumentEvent) {
        val editionData = EditionData(event)
        
        for (position in event.getDocument().getPositions(category)) {
            when {
                editionData.end < position.offset -> {
                    position.setOffset(position.offset + editionData.delta)
                }
                
                position.end < editionData.offset -> { } // do nothing
                
                position.offset <= editionData.offset && editionData.end <= position.end -> {
                    updateWithEditionInsidePosition(position, editionData)
                } 
                
                position.offset <= editionData.offset -> updateWithOverEnd(position, editionData)
                
                editionData.end <= position.end -> updateWithOverStart(position, editionData)
                
                else -> position.delete()
            }
        }
    }
    
    private fun updateWithOverStart(position: Position, editionData: EditionData) {
        val excludedLength = editionData.text
            .takeLastWhile { Character.isJavaIdentifierPart(it) }
            .length
        val deleted = editionData.end - position.offset
        val inserted = editionData.textLength - excludedLength
        position.update(editionData.offset + excludedLength, position.getLength() - deleted + inserted)
    }
    
    private fun updateWithOverEnd(position: Position, editionData: EditionData) {
        val validSymbolsLength = editionData.text
                .takeWhile { Character.isJavaIdentifierPart(it) }
                .length
        position.setLength(editionData.offset - position.offset + validSymbolsLength)
    }
    
    private fun updateWithEditionInsidePosition(position: Position, editionData: EditionData) {
        val allSymbolsValid = editionData.text.all { Character.isJavaIdentifierPart(it) }
        if (allSymbolsValid) {
            position.setLength(position.length + editionData.delta)
        } else {
            position.delete()
        }
    }
}

private val Position.end: Int
    get() = offset + length

private class EditionData(event: DocumentEvent) {
    val offset = event.offset
    val length = event.length
    val end = offset + length
    val text = event.text
    val textLength = text?.length ?: 0
    val delta = textLength - length
}

private fun Position.update(offset: Int, length: Int) {
    this.setOffset(offset)
    this.setLength(length)
}