package org.jetbrains.kotlin.testframework.utils;

import static org.jetbrains.kotlin.testframework.utils.InTextDirectivesUtils.getItems;

import java.util.List;

public class ExpectedCompletionUtils {

	private static final String EXIST_LINE_PREFIX = "EXIST:";
	private static final String ABSENT_LINE_PREFIX = "ABSENT:";
	private static final String NUMBER_LINE_PREFIX = "NUMBER:";
	private static final String EXIST_JAVA_ONLY_LINE_PREFIX = "EXIST_JAVA_ONLY:";

	public static List<String> itemsShouldAbsent(String fileText) {
		return getItems(fileText, ABSENT_LINE_PREFIX);
	}

	public static List<String> itemsShouldExist(String fileText) {
		return getItems(fileText, EXIST_LINE_PREFIX);
	}
	
	public static List<String> itemsJavaOnlyShouldExists(String fileText) {
		return getItems(fileText, EXIST_JAVA_ONLY_LINE_PREFIX);
	}

	public static Integer numberOfItemsShouldPresent(String fileText) {
		List<String> numbers = getItems(fileText, NUMBER_LINE_PREFIX);

		if (numbers.isEmpty()) {
			return null;
		}

		return new Integer(numbers.get(0));
	}
}