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
package org.jetbrains.kotlin.ui.editors;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory;

import com.intellij.openapi.util.TextRange;

public class DiagnosticAnnotation extends Annotation {

    private final TextRange range;
    private final String markedText;
    private final DiagnosticFactory<?> diagnosticFactory;
    
    public DiagnosticAnnotation(@NotNull TextRange position, @NotNull String annotationType, @NotNull String message, 
            @NotNull String markedText, @Nullable DiagnosticFactory<?> diagnosticFactory) {
        super(annotationType, true, message);
        this.range = position;
        this.markedText = markedText;
        this.diagnosticFactory = diagnosticFactory;
    }
    
    public DiagnosticAnnotation(int offset, int length, @NotNull String annotationType, @NotNull String message, 
            @NotNull String markedText, @Nullable DiagnosticFactory<?> diagnosticFactory) {
        this(new TextRange(offset, offset + length), annotationType, message, markedText, diagnosticFactory);
    }
    
    public TextRange getRange() {
        return range;
    }
    
    public int getMarkerSeverity() {
        switch (getType()) {
            case AnnotationManager.ANNOTATION_ERROR_TYPE:
                return IMarker.SEVERITY_ERROR;
                
            case AnnotationManager.ANNOTATION_WARNING_TYPE:
                return IMarker.SEVERITY_WARNING;
                
            default:
                return 0;
        }
    }
    
    public Position getPosition() {
        return new Position(range.getStartOffset(), range.getLength());
    }
    
    public String getMarkedText() {
        return markedText;
    }
    
    @Nullable
    public DiagnosticFactory<?> getDiagnostic() {
        return diagnosticFactory;
    }
}