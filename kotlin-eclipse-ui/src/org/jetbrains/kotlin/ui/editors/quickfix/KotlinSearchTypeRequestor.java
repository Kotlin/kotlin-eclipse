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
package org.jetbrains.kotlin.ui.editors.quickfix;

import java.util.List;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.TypeNameMatch;
import org.eclipse.jdt.core.search.TypeNameMatchRequestor;

public class KotlinSearchTypeRequestor extends TypeNameMatchRequestor {
    
    private final List<IType> collector;
    
    public KotlinSearchTypeRequestor(List<IType> collector) {
        this.collector = collector;
    }

    @Override
    public void acceptTypeNameMatch(TypeNameMatch match) {
        // TODO: Add visibility
        collector.add(match.getType());
    }
}