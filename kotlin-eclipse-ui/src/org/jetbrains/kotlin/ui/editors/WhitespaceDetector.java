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
package org.jetbrains.kotlin.ui.editors;

import org.eclipse.jface.text.rules.IWhitespaceDetector;
import org.jetbrains.kotlin.utils.IndenterUtil;
import org.jetbrains.kotlin.utils.LineEndUtil;

public class WhitespaceDetector implements IWhitespaceDetector {

    @Override
    public boolean isWhitespace(char c) {
        return (c == IndenterUtil.SPACE_CHAR || c == IndenterUtil.TAB_CHAR || c == LineEndUtil.NEW_LINE_CHAR || c == LineEndUtil.CARRIAGE_RETURN_CHAR);
    }
}
