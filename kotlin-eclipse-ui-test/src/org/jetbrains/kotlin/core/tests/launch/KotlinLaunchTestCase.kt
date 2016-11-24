/*******************************************************************************
* Copyright 2000-2015 JetBrains s.r.o.
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

import org.eclipse.core.runtime.NullProgressMonitor
import org.eclipse.debug.core.DebugPlugin
import org.eclipse.debug.core.ILaunch
import org.eclipse.debug.core.ILaunchListener
import org.eclipse.debug.internal.ui.DebugUIPlugin
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants
import org.eclipse.jface.dialogs.MessageDialogWithToggle
import org.jetbrains.kotlin.testframework.editor.KotlinEditorTestCase
import org.jetbrains.kotlin.testframework.utils.KotlinTestUtils
import org.jetbrains.kotlin.ui.launch.KotlinLaunchShortcut
import org.jetbrains.kotlin.ui.launch.getEntryPoint
import org.junit.After
import org.junit.Assert
import org.junit.Before

abstract class KotlinLaunchTestCase : KotlinEditorTestCase() {
    val compileWithErrorPreference = DebugUIPlugin.getDefault().getPreferenceStore()
            .getString(IInternalDebugUIConstants.PREF_CONTINUE_WITH_COMPILE_ERROR)
    
    @Before
    fun before() {
        DebugUIPlugin.getDefault().getPreferenceStore().setValue(
                IInternalDebugUIConstants.PREF_CONTINUE_WITH_COMPILE_ERROR, MessageDialogWithToggle.ALWAYS)
    }
    
    @After
    fun after() {
        DebugUIPlugin.getDefault().getPreferenceStore().setValue(
                IInternalDebugUIConstants.PREF_CONTINUE_WITH_COMPILE_ERROR, compileWithErrorPreference)
    }
    
    fun doTest(input: String, projectName: String, packageName: String, additionalSrcFolderName: String?) {
        testEditor = configureEditor("Test.kt", input, projectName, packageName)
        testEditor.getTestJavaProject().addKotlinRuntime()
        if (additionalSrcFolderName != null) {
            testEditor.getTestJavaProject().createSourceFolder(additionalSrcFolderName)
        }
        
        KotlinTestUtils.joinBuildThread()
        
        val output = launchInForeground()
        Assert.assertEquals("ok", output)
    }
    
    private fun launchInForeground(): String {
        val stdout = StringBuilder()
        val launchListener = object : ILaunchListener {
            override fun launchRemoved(launch: ILaunch) {
            }
            
            override fun launchChanged(launch: ILaunch) {
                with(launch.processes[0].streamsProxy) {
                    outputStreamMonitor.addListener { text, monitor -> stdout.append(text) }
                    errorStreamMonitor.addListener { text, monitor -> stdout.append(text) }
                }
            }
            
            override fun launchAdded(launch: ILaunch) {
            }
        }
        
        DebugPlugin.getDefault().getLaunchManager().addLaunchListener(launchListener)
        
        var launch: ILaunch? = null
        try {
            val entryPoint = getEntryPoint(getEditor().parsedFile!!)
            val launchConfiguration = KotlinLaunchShortcut.createConfiguration(entryPoint!!, testEditor.getEclipseProject())
            launch = DebugUIPlugin.buildAndLaunch(launchConfiguration, "run", NullProgressMonitor())
            
            synchronized (launch) {
                for (attempt in 0..50) {
                    if (launch!!.isTerminated) break
                    (launch as java.lang.Object).wait(100)
                }
            }
            
            if (!launch.isTerminated) stdout.append("Launch not terminated")
        } finally {
            launch?.terminate()
            DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(launchListener)
        }
        
        return stdout.toString()
    }
}