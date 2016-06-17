package org.jetbrains.kotlin.ui.tests.editors.navigation;

import org.eclipse.jdt.core.IJavaProject;
import org.jetbrains.kotlin.core.model.KotlinEnvironment;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtPsiFactory;
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
    public KtFile getParsedFile(KotlinEditor editor) {
        IJavaProject javaProject = getTestProject().getJavaProject();
        KotlinEnvironment environment = KotlinEnvironment.getEnvironment(javaProject.getProject());
        Project ideaProject = environment.getProject();
        KtFile jetFile = new KtPsiFactory(ideaProject).createFile(StringUtil.convertLineSeparators(editor.getJavaEditor().getViewer().getTextWidget().getText()));
        return jetFile;
    }
}
