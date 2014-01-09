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
package org.jetbrains.kotlin.core.launch;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.jet.cli.common.messages.CompilerMessageLocation;
import org.jetbrains.jet.cli.common.messages.CompilerMessageSeverity;

public class CompilerOutputData {
    
    private final List<CompilerOutputElement> data = new ArrayList<CompilerOutputElement>();
    
    public void add(CompilerMessageSeverity messageSeverity, String message, CompilerMessageLocation messageLocation) {
        data.add(new CompilerOutputElement(messageSeverity, message, messageLocation));
    }
    
    public void clear() {
        data.clear();
    }
    
    public List<CompilerOutputElement> getList() {
        return data;
    }
}