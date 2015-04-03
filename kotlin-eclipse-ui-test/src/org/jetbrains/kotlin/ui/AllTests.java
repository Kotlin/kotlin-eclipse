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
package org.jetbrains.kotlin.ui;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses( { 
	org.jetbrains.kotlin.ui.tests.editors.AllTests.class,
	org.jetbrains.kotlin.core.tests.launch.AllTests.class,
	org.jetbrains.kotlin.ui.tests.editors.completion.templates.KotlinTemplatesTest.class,
	org.jetbrains.kotlin.ui.tests.editors.completion.KotlinBasicCompletionTest.class,
	org.jetbrains.kotlin.ui.tests.editors.quickfix.intentions.KotlinReplaceGetIntentionTest.class,
	org.jetbrains.kotlin.ui.tests.editors.quickfix.intentions.KotlinSpecifyTypeTest.class,
	org.jetbrains.kotlin.ui.tests.editors.quickfix.autoimport.KotlinAutoImportTest.class,
	org.jetbrains.kotlin.ui.tests.editors.navigation.KotlinNavigationTest.class,
	org.jetbrains.kotlin.core.tests.diagnostics.AllDiagnosticsTests.class,
	org.jetbrains.kotlin.ui.tests.editors.completion.handlers.KotlinCompletionHandlerInsertTest.class} )
public class AllTests {
}
