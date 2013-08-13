package org.jetbrains.kotlin.testframework.utils;

import com.intellij.openapi.util.Pair;

public class SourceFile {

	private Pair<String, String> sourceFile;
	
	public SourceFile(String name, String content) {
		sourceFile = Pair.create(name, content);
	}
	
	public String getName() {
		return sourceFile.getFirst();
	}
	
	public String getContent() {
		return sourceFile.getSecond();
	}
}
