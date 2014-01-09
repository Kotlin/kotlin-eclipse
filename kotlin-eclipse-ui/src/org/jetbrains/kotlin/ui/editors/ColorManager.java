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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.ui.text.IColorManager;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public class ColorManager implements IColorManager {
    protected Map<RGB, Color> colorTable = new HashMap<>();

    @Override
    public void dispose() {
        for (Color c : colorTable.values()) {
            c.dispose();
        }
    }

    @Override
    public Color getColor(RGB rgb) {
        Color color = colorTable.get(rgb);
        if (color == null) {
            color = new Color(Display.getCurrent(), rgb);
            colorTable.put(rgb, color);
        }
        return color;
    }

    @Override
    public Color getColor(String key) {
        return null;
    }
}
