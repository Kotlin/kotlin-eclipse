package org.jetbrains.kotlin.testframework.utils;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

import com.intellij.openapi.util.io.FileUtil;

public class KotlinTestUtils {
	public enum Separator {
		TAB, SPACE;
	}
	
	public static final String ERR_TAG_OPEN = "<err>";
	public static final String ERR_TAG_CLOSE = "</err>";
	public static final String BR = "<br>";

	public static String getText(String testPath) {
		try {
			File file = new File(testPath);
			return String.valueOf(FileUtil.loadFile(file));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static String getNameByPath(String testPath) {
		return new Path(testPath).lastSegment();
	}
	
	public static void joinBuildThread() {
		while (true) {
			try {
				Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
				break;
			} catch (OperationCanceledException | InterruptedException e) {
			}
		}
	}
	
	public static void refreshWorkspace() {
		WorkspaceUtil.refreshWorkspace();
		try {
			Job.getJobManager().join(ResourcesPlugin.FAMILY_MANUAL_REFRESH, new NullProgressMonitor());
		} catch (OperationCanceledException | InterruptedException e) {
			e.printStackTrace();
		}
    }
	
	public static int getCaret(JavaEditor javaEditor) {
		return javaEditor.getViewer().getTextWidget().getCaretOffset();
	}
	
	public static String resolveTestTags(String text) {
		return text
				.replaceAll(ERR_TAG_OPEN, "")
				.replaceAll(ERR_TAG_CLOSE, "")
				.replaceAll(BR, System.lineSeparator());
    }
}
