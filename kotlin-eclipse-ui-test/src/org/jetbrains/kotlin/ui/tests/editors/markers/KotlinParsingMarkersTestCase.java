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
package org.jetbrains.kotlin.ui.tests.editors.markers;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil;
import org.jetbrains.kotlin.testframework.editor.KotlinEditorTestCase;
import org.jetbrains.kotlin.testframework.editor.KotlinEditorWithAfterFileTestCase;
import org.jetbrains.kotlin.testframework.utils.KotlinTestUtils;
import org.jetbrains.kotlin.testframework.utils.TypingUtils;
import org.jetbrains.kotlin.ui.editors.AnnotationManager;
import org.jetbrains.kotlin.ui.editors.DiagnosticAnnotation;
import org.jetbrains.kotlin.ui.editors.DiagnosticAnnotationUtil;
import org.junit.Assert;
import org.junit.Before;

public abstract class KotlinParsingMarkersTestCase extends KotlinEditorWithAfterFileTestCase {
    @Before
    public void before() {
        configureProject();
    }
    
    @Override
    protected void performTest(String fileText, String expected) {
        IFile file = getTestEditor().getEditingFile();
        
        Character typedCharacter = TypingUtils.typedCharacter(fileText);
        if (typedCharacter != null) {
            getTestEditor().type(typedCharacter);
        }
        
        KotlinPsiManager.getKotlinFileIfExist(file, EditorUtil.getSourceCode(getTestEditor().getEditor())); // We should update file because problem markers are adding manually
        for (DiagnosticAnnotation annotation : DiagnosticAnnotationUtil.INSTANCE.createParsingDiagnosticAnnotations(file)) {
            AnnotationManager.addProblemMarker(annotation, file);
        }
        
        try {
            IMarker[] markers = getTestEditor().getEditingFile().findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
            String actual = insertTagsForErrors(EditorUtil.getSourceCode(getTestEditor().getEditor()), markers);
            
            Assert.assertEquals(expected, actual);
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
    }
    
    private static String insertTagsForErrors(String fileText, IMarker[] markers) throws CoreException {
        StringBuilder result = new StringBuilder(fileText);
        
        Integer offset = 0;
        for (IMarker marker : markers) {
            int openTagStartOffset = getOpenTagStartOffset(marker);
            int closeTagStartOffset = getCloseTagStartOffset(marker);
            
            switch (marker.getAttribute(IMarker.SEVERITY, 0)) {
            case IMarker.SEVERITY_ERROR:
                offset += insertTagByOffset(result, KotlinTestUtils.ERROR_TAG_OPEN, openTagStartOffset, offset);
                offset += insertTagByOffset(result, KotlinTestUtils.ERROR_TAG_CLOSE, closeTagStartOffset, offset);
                break;
            case IMarker.SEVERITY_WARNING:
                offset += insertTagByOffset(result, KotlinEditorTestCase.WARNING_TAG_OPEN, openTagStartOffset, offset);
                offset += insertTagByOffset(result, KotlinEditorTestCase.WARNING_TAG_CLOSE, closeTagStartOffset, offset);
                break;
            default:
                break;
            }
        }
        
        return result.toString();
    }
    
    private static int getOpenTagStartOffset(IMarker marker) throws CoreException {
        return getTagStartOffset(marker, IMarker.CHAR_START);
    }
    
    private static int getCloseTagStartOffset(IMarker marker) throws CoreException {
        return getTagStartOffset(marker, IMarker.CHAR_END);
    }
    
    private static int getTagStartOffset(IMarker marker, String type) throws CoreException {
        return (int) marker.getAttribute(type);
    }
    
    private static int insertTagByOffset(StringBuilder builder, String tag, int tagStartOffset, int offset) {
    	builder.insert(tagStartOffset + offset, tag);
        return tag.length();
    }
}
