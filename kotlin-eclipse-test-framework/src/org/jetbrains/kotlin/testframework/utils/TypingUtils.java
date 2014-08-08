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

import static org.jetbrains.kotlin.testframework.utils.InTextDirectivesUtils.getItems;

import java.util.List;

public class TypingUtils {
    
    private static final String TYPE_PREFIX = "TYPE:";

    public static Character typedCharacter(String fileText) {
        List<String> items = getItems(fileText, TYPE_PREFIX);
        
        return !items.isEmpty() ? items.get(0).charAt(0) : null;
    }    
}
