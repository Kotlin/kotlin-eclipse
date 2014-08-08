package org.jetbrains.kotlin.core.tests.diagnostics;

import junit.framework.ComparisonFailure;

public class FileComparisonFailure extends ComparisonFailure {
	private static final long serialVersionUID = 1L;
	private final String myExpected;
	  private final String myActual;
	  private final String myFilePath;

	  public FileComparisonFailure(String message, String expected, String actual, String filePath) {
	    super(message, expected, actual);
	    myExpected = expected;
	    myActual = actual;
	    myFilePath = filePath;
	  }

	  public String getFilePath() {
	    return myFilePath;
	  }

	  @Override
	public String getExpected() {
	    return myExpected;
	  }

	  @Override
	public String getActual() {
	    return myActual;
	  }
	}