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
package org.jetbrains.kotlin.ui.tests.editors.quickfix.intentions;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.jetbrains.kotlin.testframework.utils.EditorTestUtils;
import org.jetbrains.kotlin.ui.editors.quickassist.KotlinSpecifyTypeAssistProposal;
import org.junit.Before;

public abstract class KotlinSpecifyTypeTestCase extends AbstractKotlinQuickAssistTestCase<KotlinSpecifyTypeAssistProposal> {
    @Override
    @Before
    public void configure() {
        configureProjectWithStdLib();
    }
    
	protected void doTest(String testPath) {
		doTestFor(testPath, new KotlinSpecifyTypeAssistProposal());
	}
	
	protected void doTestWithBuildThreadJoin(String testPath) {
		doTestFor(testPath, new KotlinSpecifyTypeAssistProposal(), true);
	}
	
	@Override
	protected void assertByEditor(JavaEditor editor, String expected) {
		EditorTestUtils.assertByEditor(editor, expected);
	}
}
