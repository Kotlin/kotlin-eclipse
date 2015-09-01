package org.jetbrains.kotlin.ui.tests.editors.navigation;

import org.eclipse.jdt.core.IJavaProject;
import org.jetbrains.kotlin.core.model.KotlinEnvironment;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.psi.JetPsiFactory;
import org.jetbrains.kotlin.ui.editors.KotlinEditor;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;

public class KotlinBuiltInsReferenceResolverTestCase extends KotlinSourcesNavigationTestCase {
    
    private static final String TEST_DATA_PATH = "common_testData/ide/resolve/builtins";

    @Override
    public String getTestDataPath() {
        return TEST_DATA_PATH;
    }

    @Override
    public JetFile getParsedFile(KotlinEditor editor) {
        IJavaProject javaProject = getTestProject().getJavaProject();
        KotlinEnvironment environment = KotlinEnvironment.getEnvironment(javaProject);
        Project ideaProject = environment.getProject();
        JetFile jetFile = new JetPsiFactory(ideaProject).createFile(StringUtil.convertLineSeparators(editor.getJavaEditor().getViewer().getTextWidget().getText()));
        return jetFile;
    }
}
