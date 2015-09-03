package org.jetbrains.kotlin.ui.tests.debugger

import org.jetbrains.kotlin.testframework.editor.KotlinProjectTestCase
import org.junit.Before
import org.jetbrains.kotlin.testframework.utils.KotlinTestUtils
import org.eclipse.debug.ui.DebugUITools
import org.jetbrains.kotlin.testframework.editor.TextEditorTest
import org.jetbrains.kotlin.ui.launch.KotlinLaunchShortcut
import org.eclipse.core.runtime.NullProgressMonitor
import org.eclipse.debug.core.ILaunch
import org.eclipse.debug.ui.actions.IToggleBreakpointsTarget
import org.eclipse.jface.text.TextSelection
import org.eclipse.debug.core.IDebugEventSetListener
import org.eclipse.debug.core.DebugEvent
import org.jetbrains.kotlin.ui.debug.commands.stepIntoSelection
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.eclipse.jface.text.ITextSelection
import org.junit.After
import org.eclipse.jdt.debug.core.IJavaStackFrame
import org.eclipse.debug.core.DebugPlugin
import org.eclipse.jdt.debug.core.IJavaThread
import org.junit.Assert
import org.jetbrains.kotlin.testframework.utils.InTextDirectivesUtils
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.ui.PlatformUI
import org.jetbrains.kotlin.testframework.editor.KotlinEditorTestCase
import org.eclipse.debug.core.ILaunchManager

abstract class KotlinStepIntoSelectionTestCase : KotlinProjectTestCase() {
    @Before
    override fun beforeTest() {
        super.beforeTest()
        configureProjectWithStdLib()
        PlatformUI.getWorkbench().showPerspective("org.eclipse.debug.ui.DebugPerspective", 
                PlatformUI.getWorkbench().getActiveWorkbenchWindow())
    }
    
    companion object {
        val BREAKPOINT_TAG = "Breakpoint!"
    }
    
    fun doTest(testPath: String) {
        val fileText = KotlinTestUtils.getText(testPath)
        val testEditor = configureEditor(KotlinTestUtils.getNameByPath(testPath), fileText)
        
        setInitialBreakpoints(testEditor)
        
        val waiter = KotlinDebugEventWaiter(DebugEvent.SUSPEND)
        
        val launch = launchToBreakpoint(testEditor)
        KotlinTestUtils.joinBuildThread()

        try {
            waiter.waitForEvent()
            
            val steppedWaiter = KotlinDebugEventWaiter(DebugEvent.SUSPEND)
            
            stepIntoSelection(
                    testEditor.getEditor() as KotlinFileEditor, 
                    testEditor.getEditor().getEditorSite().getSelectionProvider().getSelection() as ITextSelection)
                    
            val actualThread = steppedWaiter.waitForEvent()!!
            val javaStackFrame = actualThread.getTopStackFrame() as IJavaStackFrame
            
            val expectedMethodName = InTextDirectivesUtils.findStringWithPrefixes(fileText, "LINE:")
            
            Assert.assertEquals(expectedMethodName, javaStackFrame.getLineNumber().toString())
        } finally {
            if (!launch.isTerminated()) {
                val terminateWaiter = KotlinDebugEventWaiter(DebugEvent.TERMINATE)
                launch.getDebugTarget().getThreads().forEach { 
                    if (it.isSuspended()) it.resume()
                }
                
                launch.getDebugTarget().terminate()
                terminateWaiter.waitForEvent()!!
            }
            
            DebugPlugin.getDefault().getLaunchManager().removeLaunch(launch)
        }
    }
    
    fun launchToBreakpoint(testEditor: TextEditorTest): ILaunch {
        val launchConfiguration = KotlinLaunchShortcut.createConfiguration(testEditor.getEditingFile())
        return launchConfiguration.launch(ILaunchManager.DEBUG_MODE, null)
    }
    
    fun setInitialBreakpoints(testEditor: TextEditorTest) {
        val selection = testEditor.getEditor().getEditorSite().getSelectionProvider().getSelection()
        
        val toggleBreakpointsTarget = DebugUITools.getToggleBreakpointsTargetManager()
                .getToggleBreakpointsTarget(testEditor.getEditor(), selection)
        
        val document = testEditor.getDocument()
        val text = document.get()
        var breakpointOffset = text.indexOf(BREAKPOINT_TAG)
        while (breakpointOffset > 0) {
            val lineOfTag = document.getLineOfOffset(breakpointOffset)
            val offset = document.getLineOffset(lineOfTag + 1)
            toggleBreakpointsTarget.toggleLineBreakpoints(testEditor.getEditor(), TextSelection(document, offset, 0))
            
            breakpointOffset = text.indexOf(BREAKPOINT_TAG, offset)
        }
    }
}

class KotlinDebugEventWaiter(val eventType: Int) : IDebugEventSetListener {
    init {
        DebugPlugin.getDefault().addDebugEventListener(this)
    }
    
    volatile var applicableEvent: DebugEvent? = null
    
    override fun handleDebugEvents(events: Array<DebugEvent>) {
        events.firstOrNull { isApplicable(it) }?.let {
            applicableEvent = it
            unregister()
        }
    }
    
    synchronized fun waitForEvent(): IJavaThread? {
        if (applicableEvent != null) return applicableEvent!!.getSource() as IJavaThread
        
        Thread.sleep(2000)
        
        unregister()
        return applicableEvent?.getSource() as? IJavaThread
    }
    
    fun isApplicable(event: DebugEvent): Boolean {
        return event.getKind() == eventType && event.getDetail() != DebugEvent.EVALUATION_IMPLICIT
    }
    
    fun unregister() {
        DebugPlugin.getDefault().removeDebugEventListener(this)
    }
}