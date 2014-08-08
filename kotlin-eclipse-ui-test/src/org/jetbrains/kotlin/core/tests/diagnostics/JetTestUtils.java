package org.jetbrains.kotlin.core.tests.diagnostics;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.plugin.JetLanguage;
import org.jetbrains.jet.utils.UtilsPackage;
import org.junit.Assert;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.psi.PsiFileFactory;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.psi.impl.PsiFileFactoryImpl;

public class JetTestUtils {
	
    public static final Pattern FILE_OR_MODULE_PATTERN = Pattern.compile("(?://\\s*MODULE:\\s*(\\w+)(\\(\\w+(?:, \\w+)*\\))?\\s*)?" +
            "//\\s*FILE:\\s*(.*)$", Pattern.MULTILINE);

    public static final Pattern DIRECTIVE_PATTERN = Pattern.compile("^//\\s*!(\\w+)(:\\s*(.*)$)?", Pattern.MULTILINE);
	
	public static void mkdirs(File file) throws IOException {
        if (file.isDirectory()) {
            return;
        }
        if (!file.mkdirs()) {
            if (file.exists()) {
                throw new IOException("failed to create " + file + " file exists and not a directory");
            }
            throw new IOException();
        }
    }
	
	public static String doLoadFile(File file) throws IOException {
        return FileUtil.loadFile(file).trim();
    }
	
	public interface TestFileFactory<M, F> {
        F createFile(@Nullable M module, String fileName, String text, Map<String, String> directives);
        M createModule(String name, List<String> dependencies);
    }
	
	@NotNull
    public static JetFile createFile(@NotNull @NonNls String name, @NotNull String text, @NotNull Project project) {
        LightVirtualFile virtualFile = new LightVirtualFile(name, JetLanguage.INSTANCE, text);
        virtualFile.setCharset(CharsetToolkit.UTF8_CHARSET);
        return (JetFile) ((PsiFileFactoryImpl) PsiFileFactory.getInstance(project)).trySetupPsiForFile(virtualFile, JetLanguage.INSTANCE, true, false);
    }
	
	private static List<String> parseDependencies(@Nullable String dependencies) {
        if (dependencies == null) return Collections.emptyList();

        Matcher matcher = Pattern.compile("\\w+").matcher(dependencies);
        List<String> result = new ArrayList<String>();
        while (matcher.find()) {
            result.add(matcher.group());
        }
        return result;
    }
	
	public static void assertEqualsToFile(@NotNull File expectedFile, @NotNull String actual) {
        try {
        	String actualText = StringUtil.convertLineSeparators(actual.trim());
//        	TODO: add remove trailing whitespaces from expected and actual text

            if (!expectedFile.exists()) {
                FileUtil.writeToFile(expectedFile, actualText.getBytes());
                Assert.fail("Expected data file did not exist. Generating: " + expectedFile);
            }
            String expected = FileUtil.loadFile(expectedFile, "UTF-8", true);
            String expectedText = StringUtil.convertLineSeparators(expected.trim());

            if (!Comparing.equal(expectedText, actualText)) {
            	throw new FileComparisonFailure("Actual data differs from file content: " + expectedFile.getName(),
                        expected, actual, expectedFile.getAbsolutePath());
            }
        }
        catch (IOException e) {
            throw UtilsPackage.rethrow(e);
        }
    }
	
	@NotNull
    public static Map<String, String> parseDirectives(String expectedText) {
        Map<String, String> directives = Maps.newHashMap();
        Matcher directiveMatcher = DIRECTIVE_PATTERN.matcher(expectedText);
        int start = 0;
        while (directiveMatcher.find()) {
            if (directiveMatcher.start() != start) {
                Assert.fail("Directives should only occur at the beginning of a file: " + directiveMatcher.group());
            }
            String name = directiveMatcher.group(1);
            String value = directiveMatcher.group(3);
            String oldValue = directives.put(name, value);
            Assert.assertNull("Directive overwritten: " + name + " old value: " + oldValue + " new value: " + value, oldValue);
            start = directiveMatcher.end() + 1;
        }
        return directives;
    }
	
	public static <M, F> List<F> createTestFiles(String testFileName, String expectedText, TestFileFactory<M, F> factory) {
        Map<String, String> directives = parseDirectives(expectedText);

        List<F> testFiles = Lists.newArrayList();
        Matcher matcher = FILE_OR_MODULE_PATTERN.matcher(expectedText);
        if (!matcher.find()) {
            // One file
            testFiles.add(factory.createFile(null, testFileName, expectedText, directives));
        }
        else {
            int processedChars = 0;
            M module = null;
            // Many files
            while (true) {
                String moduleName = matcher.group(1);
                String moduleDependencies = matcher.group(2);
                if (moduleName != null) {
                    module = factory.createModule(moduleName, parseDependencies(moduleDependencies));
                }

                String fileName = matcher.group(3);
                int start = processedChars;

                boolean nextFileExists = matcher.find();
                int end;
                if (nextFileExists) {
                    end = matcher.start();
                }
                else {
                    end = expectedText.length();
                }
                String fileText = expectedText.substring(start, end);
                processedChars = end;

                testFiles.add(factory.createFile(module, fileName, fileText, directives));

                if (!nextFileExists) break;
            }
            assert processedChars == expectedText.length() : "Characters skipped from " +
                                                             processedChars +
                                                             " to " +
                                                             (expectedText.length() - 1);
        }
        return testFiles;
    }
}
