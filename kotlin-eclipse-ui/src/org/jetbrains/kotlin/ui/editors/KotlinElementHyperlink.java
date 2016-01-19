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

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.ui.editors.navigation.KotlinOpenDeclarationAction;

public class KotlinElementHyperlink implements IHyperlink {
    
    private static final String HYPERLINK_TEXT = "Open Kotlin Declaration";
    
    private final KotlinOpenDeclarationAction openAction;
    private final IRegion region;
    
    public KotlinElementHyperlink(@NotNull KotlinOpenDeclarationAction openAction, @NotNull IRegion region) {
        this.openAction = openAction;
        this.region = region;
    }

    @Override
    public IRegion getHyperlinkRegion() {
        return region;
    }

    @Override
    public String getTypeLabel() {
        return null;
    }

    @Override
    public String getHyperlinkText() {
        return HYPERLINK_TEXT;
    }

    @Override
    public void open() {
        openAction.run();
    }
}
