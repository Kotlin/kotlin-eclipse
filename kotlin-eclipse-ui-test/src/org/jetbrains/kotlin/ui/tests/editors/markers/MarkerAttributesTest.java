package org.jetbrains.kotlin.ui.tests.editors.markers;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Arrays;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil;
import org.jetbrains.kotlin.testframework.utils.TypingUtils;
import org.jetbrains.kotlin.ui.editors.annotations.AnnotationManager;
import org.jetbrains.kotlin.ui.editors.annotations.DiagnosticAnnotation;
import org.jetbrains.kotlin.ui.editors.annotations.DiagnosticAnnotationUtil;
import org.junit.Test;

public class MarkerAttributesTest extends KotlinParsingMarkersTestCase {
    @Override
    protected String getTestDataRelativePath() {
        return "markers/parsing";
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
            AnnotationManager.INSTANCE.addProblemMarker(annotation, file);
        }
        
        try {
            IMarker[] markers = getTestEditor().getEditingFile().findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);

            Arrays.asList(markers).forEach(marker -> {
                try {
                    assertThat(marker.getAttribute(IMarker.LOCATION), equalTo("line 3"));
                    assertThat(marker.getAttribute(IMarker.LINE_NUMBER), equalTo(3));
                } catch (CoreException e) {
                    KotlinLogger.logAndThrow(e);
                }
            });
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
    }
    
    @Test
    public void missingClosingBraceErrorTest() {
        doAutoTest();
    }
}
