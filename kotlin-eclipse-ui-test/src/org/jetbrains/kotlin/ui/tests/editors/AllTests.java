package org.jetbrains.kotlin.ui.tests.editors;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses( { 
	KotlinEditorBaseTest.class, 
	KotlinAutoIndenterTest.class,
	KotlinAnalyzerTest.class,
	KotlinHighlightningScannerTest.class,
	KotlinBracketInserterTest.class} )
public class AllTests {

}