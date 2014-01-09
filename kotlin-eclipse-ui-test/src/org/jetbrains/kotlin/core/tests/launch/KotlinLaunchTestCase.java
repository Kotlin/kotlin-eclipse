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
package org.jetbrains.kotlin.core.tests.launch;

import junit.framework.Assert;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.jetbrains.kotlin.testframework.editor.KotlinEditorTestCase;
import org.jetbrains.kotlin.ui.launch.KotlinLaunchShortcut;

public class KotlinLaunchTestCase extends KotlinEditorTestCase {
	
	public void doTest(String input, String projectName, String packageName, String additionalSrcFolderName) {
		testEditor = configureEditor("Test.kt", input, projectName, packageName);
			if (additionalSrcFolderName != null) {
				try {
					testEditor.getTestJavaProject().createSourceFolder(additionalSrcFolderName);
				} catch (CoreException e) {
					throw new RuntimeException(e);
				}
			}
		
		launchInForeground();
		
		IConsole console = findOutputConsole();
		
		Assert.assertNotNull(console);
	}
	
	private IConsole findOutputConsole() {
		IConsoleManager consoleManager = ConsolePlugin.getDefault().getConsoleManager();
		IConsole[] consoles = consoleManager.getConsoles();
		IConsole outputConsole = null;
		for (IConsole console : consoles) {
			if (IDebugUIConstants.ID_PROCESS_CONSOLE_TYPE.equals(console.getType())) {
				outputConsole = console;
				break;
			}
		}
		
		return outputConsole;
	}
	
	private void launchInForeground() {
		ILaunchConfiguration launchConfiguration = KotlinLaunchShortcut.createConfiguration(testEditor.getEditingFile());
		try {
			ILaunch launch = launchConfiguration.launch("run", new NullProgressMonitor(), true);
			while (!launch.isTerminated()) {
				Thread.sleep(100);
			}
		} catch (CoreException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}
