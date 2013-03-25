package org.jetbrains.kotlin.ui.editors;

import org.eclipse.jface.text.rules.IWhitespaceDetector;

public class KotlinWhitespaceDetector implements IWhitespaceDetector {

	public boolean isWhitespace(char c) {
		return (c == ' ' || c == '\t' || c == '\n' || c == '\r');
	}
}
