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
    
    @After
    override fun afterTest() {
        super.afterTest()
        
//        ResourcesPlugin.getWorkspace().getRoot().getProjects().forEach { it.delete(true, true, null) }
    }
    
    fun doTest(testPath: String) {
        val fileText = KotlinTestUtils.getText(testPath)
        val testEditor = configureEditor(KotlinTestUtils.getNameByPath(testPath), fileText)
//        testEditor = KotlinEditorTestCase.configureEditor(KotlinTestUtils.getNameByPath(testPath), fileText)
//        testEditor.getTestJavaProject().addKotlinRuntime()
        
        setInitialBreakpoint(testEditor)
        
        val waiter = KotlinDebugEventWaiter(DebugEvent.SUSPEND)
        
        val launch = launchToBreakpoint(testEditor)
        KotlinTestUtils.joinBuildThread()

        try {
            val thread = waiter.waitForEvent()!!
            
            val steppedWaiter = KotlinElementDebugEventWaiter(DebugEvent.SUSPEND, thread)
            
            stepIntoSelection(
                    thread.getTopStackFrame() as IJavaStackFrame,
                    testEditor.getEditor() as KotlinFileEditor, 
                    testEditor.getEditor().getEditorSite().getSelectionProvider().getSelection() as ITextSelection)
                    
            val actualThread = steppedWaiter.waitForEvent()!!
            val javaStackFrame = actualThread.getTopStackFrame() as IJavaStackFrame
            
            val expectedMethodName = InTextDirectivesUtils.findStringWithPrefixes(fileText, "LINE:")
            
            Assert.assertEquals(expectedMethodName, javaStackFrame.getLineNumber().toString())
        } finally {
            if (!launch.isTerminated()) {
                val terminateWaiter = KotlinElementDebugEventWaiter(DebugEvent.TERMINATE, launch.getDebugTarget().getThreads()[0])
                launch.getDebugTarget().getThreads().forEach { 
                    if (it.isSuspended()) it.resume()
                }
                
                launch.getDebugTarget().terminate()
                launch.getDebugTarget().disconnect()
                terminateWaiter.waitForEvent()!!
            }
            
            DebugPlugin.getDefault().getLaunchManager().removeLaunch(launch)
        
            val modelWaiter = KotlinElementDebugEventWaiter(DebugEvent.MODEL_SPECIFIC, this);
            DebugPlugin.getDefault().fireDebugEventSet(arrayOf(DebugEvent(this, DebugEvent.MODEL_SPECIFIC)));
            modelWaiter.waitForEvent();
        }
    }
    
    fun launchToBreakpoint(testEditor: TextEditorTest): ILaunch {
        val launchConfiguration = KotlinLaunchShortcut.createConfiguration(testEditor.getEditingFile())
        return launchConfiguration.launch(ILaunchManager.DEBUG_MODE, null, true)
    }
    
    fun setInitialBreakpoint(testEditor: TextEditorTest) {
        val selection = testEditor.getEditor().getEditorSite().getSelectionProvider().getSelection()
        
        val toggleBreakpointsTarget = DebugUITools.getToggleBreakpointsTargetManager()
                .getToggleBreakpointsTarget(testEditor.getEditor(), selection)
        
        toggleBreakpointsTarget.toggleLineBreakpoints(testEditor.getEditor(), 
                TextSelection(testEditor.getDocument(), testEditor.getCaretOffset(), 0))
    }
}

open class KotlinDebugEventWaiter(val eventType: Int) : IDebugEventSetListener {
    init {
        DebugPlugin.getDefault().addDebugEventListener(this)
    }
    
    @Volatile var applicableEvent: DebugEvent? = null
    
    @Synchronized override fun handleDebugEvents(events: Array<DebugEvent>) {
        events.firstOrNull { isApplicable(it) }?.let {
            unregister()
            applicableEvent = it
            (this as java.lang.Object).notifyAll()
        }
    }
    
    @Synchronized fun waitForEvent(): IJavaThread? {
        if (applicableEvent != null) return applicableEvent!!.getSource() as IJavaThread
        
        (this as java.lang.Object).wait(4000)
        
        unregister()
        return applicableEvent?.getSource() as? IJavaThread
    }
    
    open fun isApplicable(event: DebugEvent): Boolean {
        return event.getKind() == eventType && event.getDetail() != DebugEvent.EVALUATION_IMPLICIT
    }
    
    fun unregister() {
        DebugPlugin.getDefault().removeDebugEventListener(this)
    }
}

class KotlinElementDebugEventWaiter(type: Int, val element: Any) : KotlinDebugEventWaiter(type) {
    override fun isApplicable(event: DebugEvent): Boolean {
        return super.isApplicable(event) && event.getSource() == element
    }
}