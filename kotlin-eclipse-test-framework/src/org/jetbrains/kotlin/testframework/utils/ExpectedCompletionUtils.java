package org.jetbrains.kotlin.testframework.utils;

import static org.jetbrains.kotlin.testframework.utils.InTextDirectivesUtils.getItems;

import java.util.List;

public class ExpectedCompletionUtils {

	private static final String EXIST_LINE_PREFIX = "EXIST:";
	private static final String ABSENT_LINE_PREFIX = "ABSENT:";
	private static final String NUMBER_LINE_PREFIX = "NUMBER:";
	private static final String EXIST_JAVA_ONLY_LINE_PREFIX = "EXIST_JAVA_ONLY:";
	private static final String ELEMENT_PREFIX = "ELEMENT:";
	private static final String COMPLETION_CHAR_PREFIX = "COMPLETION_CHAR:";
	private static final String INVOCATION_COUNT_PREFIX = "INVOCATION_COUNT:";
	private static final String NOTHING_ELSE_PREFIX = "NOTHING_ELSE";

	public static List<String> itemsShouldAbsent(String fileText) {
		return getItems(fileText, ABSENT_LINE_PREFIX);
	}

	public static List<String> itemsShouldExist(String fileText) {
		return getItems(fileText, EXIST_LINE_PREFIX);
	}
	
	public static boolean isNothingElseExpected(String fileText) {
	    return !InTextDirectivesUtils.findListWithPrefixes(fileText, NOTHING_ELSE_PREFIX).isEmpty();
	}
	
	public static String itemToComplete(String fileText) {
		List<String> items = getItems(fileText, ELEMENT_PREFIX);
		if (!items.isEmpty()) {
			assert items.size() == 1 : "There should be only one item to complete";
			return items.get(0);
		}
		
		return null;
	}
	
	public static Character getCompletionChar(String fileText) {
		List<String> items = getItems(fileText, COMPLETION_CHAR_PREFIX);
		return !items.isEmpty() ? items.get(0).charAt(0) : null;
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
	
	public static Boolean shouldHideNonVisibleMembers(String fileText) {
		List<String> counts = getItems(fileText, INVOCATION_COUNT_PREFIX);

		if (counts.isEmpty()) {
			return true;
		}

		return new Integer(counts.get(0)) == 1;
	}
}