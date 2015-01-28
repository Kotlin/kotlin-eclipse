package org.jetbrains.kotlin.core.tests.launch;

import org.eclipse.jdt.junit.TestRunListener;
import org.eclipse.jdt.junit.model.ITestCaseElement;
import org.eclipse.jdt.junit.model.ITestRunSession;

public class KotlinJUnitTestRunListener extends TestRunListener {
	private volatile boolean isFinished = false;
	
	private volatile int testsCount;
	
	@Override
	public void sessionStarted(ITestRunSession session) {
		isFinished = false;
		testsCount = 0;
	}
	
	@Override
	public void testCaseStarted(ITestCaseElement testCaseElement) {
		testsCount++;
	}
	
	@Override
	public void sessionFinished(ITestRunSession session) {
		isFinished = true;
	}
	
	public boolean isSessionFinished() {
		return isFinished;
	}
	
	public int getTestsStartedCount() {
		return testsCount;
	}
}
