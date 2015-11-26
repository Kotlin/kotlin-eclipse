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
package org.jetbrains.kotlin.ui.editors.annotations

import org.eclipse.core.resources.IMarker
import org.eclipse.jface.text.Position
import org.eclipse.jface.text.source.Annotation
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import com.intellij.openapi.util.TextRange

class DiagnosticAnnotation(
        val offset: Int,
        val length: Int,
        val annotationType: String, 
        val message: String, 
        val markedText: String, 
        val diagnostic: DiagnosticFactory<*>?) : Annotation(annotationType, true, message) {
    
    val markerSeverity = when (getType()) {
        AnnotationManager.ANNOTATION_ERROR_TYPE -> IMarker.SEVERITY_ERROR
        AnnotationManager.ANNOTATION_WARNING_TYPE -> IMarker.SEVERITY_WARNING
        else -> 0
    }
}

val DiagnosticAnnotation.position: Position
    get() = Position(offset, length)

val DiagnosticAnnotation.endOffset: Int
    get() = offset + length