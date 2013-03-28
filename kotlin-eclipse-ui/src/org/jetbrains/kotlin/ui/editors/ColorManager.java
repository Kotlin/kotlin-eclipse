package org.jetbrains.kotlin.ui.editors;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public class ColorManager {
    protected Map<RGB, Color> colorTable = new HashMap<>();

    public void dispose() {
        for (Color c : colorTable.values()) {
            c.dispose();
        }
    }

    public Color getColor(RGB rgb) {
        Color color = colorTable.get(rgb);
        if (color == null) {
            color = new Color(Display.getCurrent(), rgb);
            colorTable.put(rgb, color);
        }
        return color;
    }
}
