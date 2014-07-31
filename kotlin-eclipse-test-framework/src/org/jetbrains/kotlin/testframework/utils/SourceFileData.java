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
package org.jetbrains.kotlin.testframework.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SourceFileData {
    
    private final static String DEFAULT_PACKAGE_NAME = "";
    private final static String JAVA_IDENTIFIER_REGEXP = "[_a-zA-Z]\\w*";
    private final static String PACKAGE_REGEXP = "package\\s((" + JAVA_IDENTIFIER_REGEXP + ")(\\." + JAVA_IDENTIFIER_REGEXP + ")*)";
    private final static Pattern PATTERN = Pattern.compile(PACKAGE_REGEXP);
    
    private final String fileName;
    private final String packageName;
    private final String content;

    public SourceFileData(String fileName, String content) {
        this.fileName = fileName;
        this.content = content;
        this.packageName = getPackageFromContent(content);
    }
    
    public String getFileName() {
        return fileName; 
    }
    
    public String getPackageName() {
        return packageName;
    }

    public String getContent() {
        return content;
    }

    public static String getPackageFromContent(String content) {
        Matcher matcher = PATTERN.matcher(content);
        return matcher.find() ? matcher.group(1) : DEFAULT_PACKAGE_NAME;
    }
}