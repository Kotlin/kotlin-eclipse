package org.jetbrains.kotlin.ui.tests.editors;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.kotlin.testframework.utils.SourceFile;
import org.junit.Test;

public class KotlinUnresolvedClassFixTest extends KotlinUnresolvedClassFixTestCase {
	
	List<SourceFile> NO_REFERENCE_FILES = new ArrayList<SourceFile>();

	@Test
	public void importOneStandardClass() {
		doTest("fun test() = HashSet", 
				NO_REFERENCE_FILES,

				"import java.util.HashSet<br>" +
				"<br>" +
				"fun test() = HashSet");
	}

	@Test
	public void importWithExtraBreakline() {
		doTest(
				"package testing<br>" +
				"fun test() = HashSet",
				NO_REFERENCE_FILES,

				"package testing<br>" +
				"<br>" +
				"import java.util.HashSet<br>" +
				"<br>" +
				"fun test() = HashSet");
	}

	@Test
	public void importClassWithExistingPackageKeyword() {
		doTest(
				"package testing<br>" +
				"<br>" +
				"fun test() = HashSet",
				NO_REFERENCE_FILES,

				"package testing<br>" +
				"<br>" +
				"import java.util.HashSet<br>" +
				"<br>" +
				"fun test() = HashSet");
	}

	@Test
	public void importLocalJavaClass() {
		List<SourceFile> files = new ArrayList<>();
		files.add(new SourceFile(
				"JavaClass1.java", 

				"package testing;<br>" +
				"public class JavaClass1 {}"));

		doTest("fun test() = JavaClass1", 
				files,

				"import testing.JavaClass1<br>" +
				"<br>" +
				"fun test() = JavaClass1"); 
	}
	
	@Test
	public void importLocalJavaInterface() {
		List<SourceFile> files = new ArrayList<>();
		files.add(new SourceFile(
				"JavaInterface1.java", 

				"package testing;<br>" +
				"public interface JavaInterface1 {}"));

		doTest("fun test() = JavaInterface1", 
				files,

				"import testing.JavaInterface1<br>" +
				"<br>" +
				"fun test() = JavaInterface1"); 	
	}
	
	@Test
	public void importLocalJavaEnum() {
		List<SourceFile> files = new ArrayList<>();
		files.add(new SourceFile(
				"JavaEnum1.java", 

				"package testing;<br>" +
				"public enum JavaEnum1 {}"));

		doTest("fun test() = JavaEnum1", 
				files,

				"import testing.JavaEnum1<br>" +
				"<br>" +
				"fun test() = JavaEnum1"); 
	}

	@Test
	public void fixOnlyUnresolvedReferenceExpressions() {
		doTest("fun test() = Object",
				NO_REFERENCE_FILES,
				"fun test() = Object");
	}
}