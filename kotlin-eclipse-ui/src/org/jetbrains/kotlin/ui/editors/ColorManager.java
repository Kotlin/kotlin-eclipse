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
