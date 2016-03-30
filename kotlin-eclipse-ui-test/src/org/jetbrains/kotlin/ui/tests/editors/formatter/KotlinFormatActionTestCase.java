/*******************************************************************************
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *******************************************************************************/
package org.jetbrains.kotlin.ui.tests.editors.formatter;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings;
import org.jetbrains.kotlin.testframework.editor.KotlinEditorWithAfterFileTestCase;
import org.jetbrains.kotlin.testframework.utils.EditorTestUtils;
import org.jetbrains.kotlin.testframework.utils.InTextDirectivesUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;

import kotlin.Pair;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;
import org.jetbrains.kotlin.ui.formatter.KotlinFormatterKt;

public abstract class KotlinFormatActionTestCase extends KotlinEditorWithAfterFileTestCase {
    @Before
    public void before() {
        configureProject();
    }
    
    @After
    public void setDefaultSettings() {
        KotlinFormatterKt.setSettings(new CodeStyleSettings());
    }
    
    @Override
    protected void performTest(String fileText, String content) {
    	String expectedLineDelimiter = TextUtilities.getDefaultLineDelimiter(getTestEditor().getDocument());
    	
        EditorsUI.getPreferenceStore().setValue(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS, true);
        EditorsUI.getPreferenceStore().setValue(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH, 4);
        
        configureSettings(fileText);
        
        getTestEditor().runFormatAction();
        
        EditorTestUtils.assertByEditor(getTestEditor().getEditor(), content);
        assertLineDelimiters(expectedLineDelimiter, getTestEditor().getDocument());
    }
    
    private void assertLineDelimiters(String expectedLineDelimiter, IDocument document) {
    	try {
    		for (int i = 0; i < document.getNumberOfLines() - 1; ++i) {
    			Assert.assertEquals(expectedLineDelimiter, document.getLineDelimiter(i));
    		}
    	} catch (BadLocationException e) {
    		throw new RuntimeException(e);
    	}
	}
    
    public static void configureSettings(String fileText) {
        List<String> settingsToTrue = InTextDirectivesUtils.findListWithPrefixes(fileText, "SET_TRUE:");
        List<String> settingsToFalse = InTextDirectivesUtils.findListWithPrefixes(fileText, "SET_FALSE:");
        List<Pair> settingsToIntValue = CollectionsKt.map(InTextDirectivesUtils.findListWithPrefixes(fileText, "SET_INT:"), new Function1<String, Pair>() {
            @Override
            public Pair<String, Integer> invoke(String s) {
                String[] tokens = s.split("=");
                return new Pair<String, Integer>(tokens[0].trim(), Integer.valueOf(tokens[1].trim()));
            }
        });
        
        KotlinCodeStyleSettings kotlinSettings = KotlinFormatterKt.getSettings().getCustomSettings(KotlinCodeStyleSettings.class);
        CommonCodeStyleSettings commonSettings = KotlinFormatterKt.getSettings().getCommonSettings(KotlinLanguage.INSTANCE);
        
        List<Object> objects = Arrays.asList(kotlinSettings, commonSettings);
        
        for (String trueSetting : settingsToTrue) {
            setBooleanSetting(trueSetting, true, objects);
        }

        for (String falseSetting : settingsToFalse) {
            setBooleanSetting(falseSetting, false, objects);
        }

        for (Pair<String, Integer> setting : settingsToIntValue) {
            setIntSetting(setting.getFirst(), setting.getSecond(), objects);
        }
    }
    
    public static void setBooleanSetting(String setting, Boolean value, List<Object> objects) {
        setSettingValue(setting, value, boolean.class, objects);
    }

    public static void setIntSetting(String setting, Integer value, List<Object> objects) {
        setSettingValue(setting, value, int.class, objects);
    }
    
    public static void setSettingValue(String settingName, Object value, Class<?> valueType, List<Object> objects) {
        for (Object object : objects) {
            if (setSettingWithField(settingName, object, value) || setSettingWithMethod(settingName, object, value, valueType)) {
                return;
            }
        }

        throw new IllegalArgumentException(String.format(
                "There's no property or method with name '%s' in given objects: %s", settingName, objects));
    }
    
    private static boolean setSettingWithField(String settingName, Object object, Object value) {
        try {
            Field field = object.getClass().getDeclaredField(settingName);
            field.set(object, value);
            return true;
        }
        catch (IllegalAccessException e) {
            throw new IllegalArgumentException(String.format("Can't set property with the name %s in object %s", settingName, object));
        }
        catch (NoSuchFieldException e) {
            // Do nothing - will try other variants
        }

        return false;
    }
    
    private static boolean setSettingWithMethod(String setterName, Object object, Object value, Class<?> valueType) {
        try {
            Method method = object.getClass().getMethod(setterName, valueType);
            method.invoke(object, value);
            return true;
        }
        catch (InvocationTargetException e) {
            throw new IllegalArgumentException(String.format("Can't call method with name %s for object %s", setterName, object));
        }
        catch (IllegalAccessException e) {
            throw new IllegalArgumentException(String.format("Can't access to method with name %s for object %s", setterName, object));
        }
        catch (NoSuchMethodException e) {
            // Do nothing - will try other variants
        }

        return false;
    }
}