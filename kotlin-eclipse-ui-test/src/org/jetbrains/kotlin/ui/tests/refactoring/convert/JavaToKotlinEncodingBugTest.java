/*******************************************************************************
 * Copyright 2000-2017 JetBrains s.r.o.
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
package org.jetbrains.kotlin.ui.tests.refactoring.convert;

import java.nio.charset.Charset;
import java.util.Collections;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.ISources;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.jetbrains.kotlin.testframework.editor.KotlinProjectTestCase;
import org.jetbrains.kotlin.testframework.utils.EditorTestUtils;
import org.jetbrains.kotlin.testframework.utils.TestJavaProject;
import org.jetbrains.kotlin.ui.commands.j2k.JavaToKotlinActionHandler;
import org.junit.Before;
import org.junit.Test;

public class JavaToKotlinEncodingBugTest extends KotlinProjectTestCase {

    private static final String CHARSET_UTF8 = "UTF-8";
    private static final String CHARSET_WINDOWS1251 = "Windows-1251";
    private static final String SOURCE_FILE_NAME = "Test.java";
    private static final String PROBLEMATIC_TEXT =
        "/**\n" +
        " * FIXME: \u0422\u0435\u0441\u0442\n" + // "Test" in Russian.
        " */\n";
    private static final String SOURCE_FILE_CONTENT =
        PROBLEMATIC_TEXT +
        "public class Test {\n" +
        "\n" +
        "  public static void main(String... args) {\n" +
        "    System.out.println(\"Test\");\n" +
        "  }\n" +
        "\n" +
        "}\n";
    private static final String EXPECTED_FILE_CONTENT =
        PROBLEMATIC_TEXT +
        "object Test {\n" +
        "    fun main(vararg args: String) {\n" +
        "        System.out.println(\"Test\")\n" +
        "    }\n" +
        "}";

    public void testJavaToKotlinConversion(String projectDefaultEncoding, String sourceFileEncoding, String source, String expected) throws Exception {
        TestJavaProject project = getTestProject();
        project.getJavaProject().getProject().setDefaultCharset(projectDefaultEncoding, null);

        IFile sourceFile = project.createSourceFile("", SOURCE_FILE_NAME, source, Charset.forName(sourceFileEncoding));

        EvaluationContext context = new EvaluationContext(null, new Object());
        ICompilationUnit sourceCompilaitonUnit = JavaCore.createCompilationUnitFrom(sourceFile);
        context.addVariable(ISources.ACTIVE_MENU_SELECTION_NAME, new StructuredSelection(sourceCompilaitonUnit));
        ExecutionEvent event = new ExecutionEvent(null, Collections.emptyMap(), null, context);
        JavaToKotlinActionHandler handler = new JavaToKotlinActionHandler();
        handler.execute(event);

        IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        JavaEditor activeEditor = (JavaEditor) workbenchWindow.getActivePage().getActiveEditor();
        EditorTestUtils.assertByEditor(activeEditor, expected);
    }

    @Before
    public void before() {
        configureProjectWithStdLib();
    }

    @Test
    public void test_UTF8_UTF8() throws Exception {
        testJavaToKotlinConversion(CHARSET_UTF8, CHARSET_UTF8, SOURCE_FILE_CONTENT, EXPECTED_FILE_CONTENT);
    }

    @Test
    public void test_UTF8_WIN1251() throws Exception {
        testJavaToKotlinConversion(CHARSET_UTF8, CHARSET_WINDOWS1251, SOURCE_FILE_CONTENT, EXPECTED_FILE_CONTENT);
    }

    @Test
    public void test_WIN1251_UTF8() throws Exception {
        testJavaToKotlinConversion(CHARSET_WINDOWS1251, CHARSET_UTF8, SOURCE_FILE_CONTENT, EXPECTED_FILE_CONTENT);
    }

    @Test
    public void test_WIN1251_WIN1251() throws Exception {
        testJavaToKotlinConversion(CHARSET_WINDOWS1251, CHARSET_WINDOWS1251, SOURCE_FILE_CONTENT, EXPECTED_FILE_CONTENT);
    }

}