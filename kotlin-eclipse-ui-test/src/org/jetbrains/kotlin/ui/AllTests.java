package org.jetbrains.kotlin.ui;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses( { 
	org.jetbrains.kotlin.ui.tests.editors.AllTests.class,
	org.jetbrains.kotlin.core.tests.launch.KotlinLaunchTest.class} )
public class AllTests {
}
