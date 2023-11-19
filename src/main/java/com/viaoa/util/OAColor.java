package com.viaoa.util;

import java.awt.Color;

public class OAColor {

    public static Color getForeground(Color c) {
        if (c == null) return Color.white;
        return getForegroundColor(c.getRed(), c.getGreen(), c.getBlue());
    }
    
    public static Color getForegroundColor(Color c) {
        if (c == null) return Color.white;
        return getForegroundColor(c.getRed(), c.getGreen(), c.getBlue());
    }
    
    public static Color getForegroundColor(int r, int g, int b) {
        float f = (0.2126f*r) + (0.7152f*g) + (0.0722f*b);
        int x = (f < 140) ? (0 | 0xFFFFFF) : 0;
        return new Color(x);
    }
    
}
