package org.jetbrains.kotlin.testframework.utils;

import java.util.ArrayList;
import java.util.List;

public class ExpectedCompletionUtils {

	private static final String EXIST_LINE_PREFIX = "EXIST:";
	private static final String ABSENT_LINE_PREFIX = "ABSENT:";
	private static final String NUMBER_LINE_PREFIX = "NUMBER:";

	public static List<String> itemsShouldAbsent(String fileText) {
		return getItems(fileText, ABSENT_LINE_PREFIX);
	}

	public static List<String> itemsShouldExist(String fileText) {
		return getItems(fileText, EXIST_LINE_PREFIX);
	}

	public static Integer numberOfItemsShouldPresent(String fileText) {
		List<String> numbers = getItems(fileText, NUMBER_LINE_PREFIX);

		if (numbers.isEmpty()) {
			return null;
		}

		return new Integer(numbers.get(0));
	}

//	TODO: Parse JSon string from tests
	private static List<String> getItems(String fileText, String prefix) {
		List<String> items = new ArrayList<String>();
		for (String itemStr : InTextDirectivesUtils.findLinesWithPrefixesRemoved(fileText, prefix)) {
			if (itemStr.startsWith("{")) continue;

			for (String item : itemStr.split(",")) {
				items.add(item.trim());
			}
		}

		return items;
	}
}