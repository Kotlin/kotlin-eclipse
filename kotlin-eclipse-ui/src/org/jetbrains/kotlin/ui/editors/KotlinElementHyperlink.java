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

import org.eclipse.core.runtime.Assert;
import org.eclipse.jdt.ui.actions.SelectionDispatchAction;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.viewers.StructuredSelection;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;

public class KotlinElementHyperlink implements IHyperlink {
    
    private static final String HYPERLINK_TEXT_FORMAT = "Open Kotlin %s";
    private static final String OPEN_DECLARATION_TEXT = "Declaration";
    private static final String DEFAULT_TEXT = "";
    
    private final JetReferenceExpression expression;
    private final SelectionDispatchAction openAction;
    private final IRegion region;
    
    public KotlinElementHyperlink(JetReferenceExpression expression, SelectionDispatchAction openAction, IRegion region) {
        Assert.isNotNull(expression);
        Assert.isNotNull(openAction);
        Assert.isNotNull(region);
        
        this.expression = expression;
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
        String text = openAction instanceof OpenDeclarationAction ? OPEN_DECLARATION_TEXT : DEFAULT_TEXT;
        return String.format(HYPERLINK_TEXT_FORMAT, text);
    }

    @Override
    public void open() {
        openAction.run(new StructuredSelection(expression));
    }
}
