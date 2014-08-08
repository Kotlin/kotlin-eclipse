package org.jetbrains.kotlin.core.tests.diagnostics;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses( {
	KotlinDiagnosticsJavaPlusKotlinTest.class,
	KotlinDiagnosticsTest.class
} )
public class AllDiagnosticsTests {

}