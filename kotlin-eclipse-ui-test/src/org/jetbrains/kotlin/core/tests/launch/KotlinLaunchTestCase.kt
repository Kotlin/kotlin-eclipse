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
package org.jetbrains.kotlin.core.tests.launch

import org.eclipse.core.resources.WorkspaceJob
import org.eclipse.core.runtime.CoreException
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.Status
import org.eclipse.core.runtime.SubProgressMonitor
import org.eclipse.core.runtime.jobs.Job
import org.eclipse.debug.core.ILaunchConfiguration
import org.eclipse.debug.ui.IDebugUIConstants
import org.eclipse.ui.console.ConsolePlugin
import org.eclipse.ui.console.IConsole
import org.eclipse.ui.console.IConsoleManager
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.testframework.editor.KotlinEditorTestCase
import org.jetbrains.kotlin.ui.launch.KotlinLaunchShortcut
import org.junit.Assert

abstract class KotlinLaunchTestCase : KotlinEditorTestCase() {
    fun doTest(input: String, projectName: String, packageName: String, additionalSrcFolderName: String?) {
        testEditor = configureEditor("Test.kt", input, projectName, packageName)
        testEditor.getTestJavaProject().addKotlinRuntime()
        if (additionalSrcFolderName != null) {
            testEditor.getTestJavaProject().createSourceFolder(additionalSrcFolderName)
        }
        
        launchInForeground()
        Assert.assertNotNull(findOutputConsole())
    }
    
    private fun findOutputConsole(): IConsole? {
        val consoleManager = ConsolePlugin.getDefault().getConsoleManager()
        return consoleManager.getConsoles().find { it.getType() == IDebugUIConstants.ID_PROCESS_CONSOLE_TYPE }
    }
    
    private fun launchInForeground() {
        val launchConfiguration = KotlinLaunchShortcut.createConfiguration(testEditor.getEditingFile())
        val job = object : WorkspaceJob("test") {
            override fun runInWorkspace(monitor: IProgressMonitor?):IStatus? {
                monitor!!.beginTask("test started", 1)
                try {
                    launchConfiguration!!.launch("run", SubProgressMonitor(monitor, 1), true)
                } catch (e: CoreException) {
                    KotlinLogger.logAndThrow(e)
                    return Status.CANCEL_STATUS
                } finally {
                    monitor.done()
                }
                
                return Status.OK_STATUS
            }
        }
        
        joinBuildThread()
        job.schedule()
        job.join()
    }
}